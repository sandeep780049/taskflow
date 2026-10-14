package com.taskflow.worker;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkerRepository extends JpaRepository<Worker, UUID> {

    Optional<Worker> findByName(String name);

    List<Worker> findByStatus(WorkerStatus status);

    long deleteByStatus(WorkerStatus status);

    List<Worker> findByLastHeartbeatAtBefore(OffsetDateTime cutoff);

    List<Worker> findByStatusAndLastHeartbeatAtBefore(WorkerStatus status, OffsetDateTime cutoff);

    long countByStatus(WorkerStatus status);
}
