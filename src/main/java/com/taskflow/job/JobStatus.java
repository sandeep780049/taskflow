package com.taskflow.job;

public enum JobStatus {
    SCHEDULED, QUEUED, RUNNING, SUCCEEDED, FAILED, RETRY_WAIT, CANCELLED, DEAD_LETTER;

    /** Legal terminal states from which no further transitions may occur. */
    public static final java.util.Set<JobStatus> TERMINAL_ADMINS = java.util.Set.of(SUCCEEDED, CANCELLED, DEAD_LETTER);
}
