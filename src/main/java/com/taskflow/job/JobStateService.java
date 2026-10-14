package com.taskflow.job;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

import static com.taskflow.job.JobStatus.*;

/**
 * Explicit, validated state machine for the job lifecycle.
 *
 * SCHEDULED ──> QUEUED ──> RUNNING ──> SUCCEEDED
 *                 ▲            ├──> FAILED (kept for fatal errors)
 *                 └── RETRY_WAIT┘
 * Any of SCHEDULED/QUEUED/RUNNING can go to CANCELLED; DEAD_LETTER is the
 * end state for jobs whose retry budget was exhausted.
 */
@Service
public class JobStateService {

    private static final Map<JobStatus, Set<JobStatus>> LEGAL = Map.of(
            SCHEDULED, Set.of(QUEUED, CANCELLED),
            QUEUED, Set.of(RUNNING, CANCELLED),
            RUNNING, Set.of(SUCCEEDED, FAILED, RETRY_WAIT, DEAD_LETTER, CANCELLED),
            RETRY_WAIT, Set.of(QUEUED, CANCELLED),
            SUCCEEDED, Set.of(),
            FAILED, Set.of(QUEUED, RETRY_WAIT, DEAD_LETTER),
            CANCELLED, Set.of(),
            DEAD_LETTER, Set.of()
    );

    public void assertTransition(JobStatus from, JobStatus to) {
        Set<JobStatus> allowed = LEGAL.get(from);
        if (allowed == null || !allowed.contains(to)) {
            throw com.taskflow.common.ApiException.badRequest(
                    "Illegal job status transition: " + from + " -> " + to);
        }
    }
}
