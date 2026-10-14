package com.taskflow.job;

import com.fasterxml.jackson.databind.JavaType;
import com.taskflow.common.ApiException;
import com.taskflow.engine.HandlersRegistry;
import com.taskflow.execution.JobExecutionRepository;
import com.taskflow.execution.JobLogLevel;
import com.taskflow.execution.JobLogRepository;
import com.taskflow.execution.JobLog;
import com.taskflow.job.dto.CreateJobRequest;
import com.taskflow.user.UserAccount;
import com.taskflow.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class JobServiceTest {

    private JobRepository jobs;
    private UserRepository users;
    private JobExecutionRepository executions;
    private JobLogRepository logs;
    private HandlersRegistry handlers;
    private JobService service;

    @BeforeEach
    void setUp() {
        jobs = mock(JobRepository.class);
        users = mock(UserRepository.class);
        executions = mock(JobExecutionRepository.class);
        logs = mock(JobLogRepository.class);
        handlers = mock(HandlersRegistry.class);
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        service = new JobService(jobs, users, executions, logs, handlers, mapper);
        when(jobs.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
        when(logs.save(any(JobLog.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private CreateJobRequest request(String jobType, OffsetDateTime scheduledAt, String cron) {
        return new CreateJobRequest("nightly-report", null, jobType, null,
                null, scheduledAt, cron, null, null, null, "key-1");
    }

    private UserAccount owner(UUID ownerId) {
        var user = new UserAccount("user@example.com", "hash", com.taskflow.user.Role.USER);
        try {
            var f = UserAccount.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(user, ownerId);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return user;
    }

    @Test
    void futureScheduleStartsInScheduledState() {
        UUID ownerId = UUID.randomUUID();
        when(users.getReferenceById(ownerId)).thenReturn(owner(ownerId));
        when(jobs.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(ownerId, request("ECHO", OffsetDateTime.now().plusHours(1), null));

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobs).save(captor.capture());
        Job saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(JobStatus.SCHEDULED);
        assertThat(saved.getNextAttemptAt()).isEqualTo(saved.getScheduledAt());
    }

    @Test
    void immediateJobStartsQueued() {
        UUID ownerId = UUID.randomUUID();
        when(users.getReferenceById(ownerId)).thenReturn(owner(ownerId));

        service.create(ownerId, request("ECHO", null, null));

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobs).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(JobStatus.QUEUED);
    }

    @Test
    void rejectsPastScheduleAndBadCron() {
        UUID ownerId = UUID.randomUUID();
        when(users.getReferenceById(ownerId)).thenReturn(owner(ownerId));
        assertThatThrownBy(() -> service.create(ownerId, request("ECHO", OffsetDateTime.now().minusDays(1), null)))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.create(ownerId, request("ECHO", null, "not-a-cron")))
                .isInstanceOf(ApiException.class);
        verify(jobs, never()).save(any());
    }

    @Test
    void unknownJobTypeIsRejected() {
        UUID ownerId = UUID.randomUUID();
        when(users.getReferenceById(ownerId)).thenReturn(owner(ownerId));
        org.mockito.Mockito.doThrow(ApiException.badRequest("Unknown job type: ECHO"))
                .when(handlers).validatePayload(eq("ECHO"), any());
        assertThatThrownBy(() -> service.create(ownerId, request("ECHO", null, null)))
                .isInstanceOf(ApiException.class);
        verify(jobs, never()).save(any());
    }

    @Test
    void cancelMarksRunningJobWithRequest() {
        UUID jobId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        var user = owner(ownerId);
        Job running = new Job();
        running.setId(jobId);
        running.setOwner(user);
        running.setStatus(JobStatus.RUNNING);
        when(jobs.findWithOwnerById(jobId)).thenReturn(Optional.of(running));
        when(jobs.findOwnerId(jobId)).thenReturn(Optional.of(ownerId));
        when(jobs.requestCancel(jobId)).thenReturn(1);

        boolean accepted = service.cancel(ownerId, false, jobId);
        assertThat(accepted).isTrue();
        verify(jobs).requestCancel(jobId);
    }

    @Test
    void accessIsDeniedForOtherOwnersWithoutLeaks() {
        UUID jobId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        when(jobs.findOwnerId(jobId)).thenReturn(Optional.of(otherId));
        assertThatThrownBy(() -> service.assertAccess(ownerId, false, jobId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not found");
    }
}
