package com.taskflow.job;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    Page<Job> findByOwnerId(UUID ownerId, Pageable pageable);

    Page<Job> findByOwnerIdAndStatus(UUID ownerId, JobStatus status, Pageable pageable);

    Page<Job> findByStatus(JobStatus status, Pageable pageable);

    Optional<Job> findWithOwnerById(UUID id);

    // --- atomic state updates (all conditional; return rows affected) ---

    /** Scheduler tick: promote SCHEDULED jobs whose time has come. */
    @Modifying
    @Query(value = """
            UPDATE jobs SET status = 'QUEUED', updated_at = now()
            WHERE status = 'SCHEDULED' AND scheduled_at <= now()
            """, nativeQuery = true)
    int promoteDueScheduledJobs();

    /** Recovery/queue tick: move RETRY_WAIT jobs whose backoff expired into QUEUED. */
    @Modifying
    @Query(value = """
            UPDATE jobs SET status = 'QUEUED', updated_at = now()
            WHERE status = 'RETRY_WAIT' AND next_attempt_at <= now()
            """, nativeQuery = true)
    int promoteRetryWaitingJobs();

    /**
     * Atomically claim one due job using row locking. The SELECT ... FOR UPDATE
     * SKIP LOCKED pattern lets many workers poll the same table concurrently
     * without contention: each worker locks at most one row and skips rows
     * claimed by others. The lock is released as soon as this transaction
     * commits; further safety comes from the lease (locked_until) while RUNNING.
     *
     * Called inside a short transaction; the returned id is then claimed in a
     * second update within the same transaction.
     */
    @Query(value = """
            SELECT id FROM jobs
            WHERE status = 'QUEUED'
              AND next_attempt_at <= now()
              AND (locked_until IS NULL OR locked_until < now())
              AND cancel_requested = FALSE
            ORDER BY priority DESC, next_attempt_at ASC
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<UUID> findClaimableJobId();

    @Modifying
    @Query(value = """
            UPDATE jobs
            SET status = 'RUNNING', locked_by = :worker, locked_until = :leaseUntil,
                attempts = attempts + 1, updated_at = now()
            WHERE id = :jobId
            """, nativeQuery = true)
    int claimJob(@Param("jobId") UUID jobId, @Param("worker") String worker,
                 @Param("leaseUntil") OffsetDateTime leaseUntil);

    /** Renew the lease while a job runs (used by the heartbeat loop). */
    @Modifying
    @Query(value = """
            UPDATE jobs SET locked_until = :leaseUntil, updated_at = now()
            WHERE id = :jobId AND status = 'RUNNING' AND locked_by = :worker
            """, nativeQuery = true)
    int renewLease(@Param("jobId") UUID jobId, @Param("worker") String worker,
                   @Param("leaseUntil") OffsetDateTime leaseUntil);

    /** Mark a completed/failed attempt and set the new status atomically. */
    @Modifying
    @Query(value = """
            UPDATE jobs
            SET status = :#{#status.name()}, locked_by = NULL, locked_until = NULL,
                next_attempt_at = :nextAttemptAt, updated_at = now()
            WHERE id = :jobId AND status = 'RUNNING' AND locked_by = :worker
            """, nativeQuery = true)
    int completeAttempt(@Param("jobId") UUID jobId, @Param("worker") String worker,
                        @Param("status") JobStatus status,
                        @Param("nextAttemptAt") OffsetDateTime nextAttemptAt);

    /** Cancel non-running jobs unconditionally (owner action). */
    @Modifying
    @Query(value = """
            UPDATE jobs SET status = 'CANCELLED', locked_by = NULL, locked_until = NULL, updated_at = now()
            WHERE id = :jobId AND status IN ('SCHEDULED', 'QUEUED', 'RETRY_WAIT')
            """, nativeQuery = true)
    int cancelWaiting(@Param("jobId") UUID jobId);

    /** Graceful cancel of a running job: request it, the worker checks mid-run. */
    @Modifying
    @Query(value = "UPDATE jobs SET cancel_requested = TRUE, updated_at = now() WHERE id = :jobId AND status = 'RUNNING'",
            nativeQuery = true)
    int requestCancel(@Param("jobId") UUID jobId);

    /** Read the cancel flag for a job the current executor owns. */
    @Query(value = "SELECT cancel_requested FROM jobs WHERE id = :jobId", nativeQuery = true)
    boolean isCancelRequestedRaw(@Param("jobId") UUID jobId);

    /** Recover jobs whose worker lease expired (worker crashed). */
    @Query(value = """
            SELECT id FROM jobs
            WHERE status = 'RUNNING' AND locked_until < now()
            LIMIT :limit
            """, nativeQuery = true)
    List<UUID> findExpiredLeaseIds(@Param("limit") int limit);

    /** Get the owning user id for access checks. */
    @Query("SELECT j.owner.id FROM Job j WHERE j.id = :jobId")
    Optional<UUID> findOwnerId(@Param("jobId") UUID jobId);

    /**
     * Commissioner note: metrics rollups read all jobs; filtering for
     * non-admin users happens in the service query below.
     */
    @Query(value = "SELECT status, count(*) FROM jobs GROUP BY status", nativeQuery = true)
    List<Object[]> countAllByStatusRaw();

    @Query(value = """
            SELECT status, count(*) FROM jobs WHERE owner_id = :ownerId GROUP BY status
            """, nativeQuery = true)
    List<Object[]> countByStatusRaw(@Param("ownerId") UUID ownerId);

    long deleteByIdAndOwnerId(UUID id, UUID ownerId);

    /** Recurring jobs: attempts count per occurrence, reset after every requeue. */
    @Modifying
    @Query(value = "UPDATE jobs SET attempts = 0 WHERE id = :jobId", nativeQuery = true)
    int resetAttemptsAfterRequeue(@Param("jobId") UUID jobId);

    /**
     * Lease recovery: return a RUNNING job whose lease expired to the queue
     * (or dead-letter it when the retry budget was exhausted).
     */
    @Modifying
    @Query(value = """
            UPDATE jobs
            SET status = :#{#status.name()}, locked_by = NULL, locked_until = NULL,
                next_attempt_at = :nextAttemptAt, updated_at = now()
            WHERE id = :jobId AND status = 'RUNNING' AND locked_until < now()
            """, nativeQuery = true)
    int recoverExpiredLease(@Param("jobId") UUID jobId, @Param("status") JobStatus status,
                            @Param("nextAttemptAt") OffsetDateTime nextAttemptAt);
}
