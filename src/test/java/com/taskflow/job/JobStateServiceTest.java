package com.taskflow.job;

import com.taskflow.common.ApiException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobStateServiceTest {

    private final JobStateService states = new JobStateService();

    @Test
    void happyPathIsLegal() {
        states.assertTransition(JobStatus.SCHEDULED, JobStatus.QUEUED);
        states.assertTransition(JobStatus.QUEUED, JobStatus.RUNNING);
        states.assertTransition(JobStatus.RUNNING, JobStatus.SUCCEEDED);
    }

    @Test
    void failureThenRetryThenDeadLetterIsLegal() {
        states.assertTransition(JobStatus.RUNNING, JobStatus.RETRY_WAIT);
        states.assertTransition(JobStatus.RETRY_WAIT, JobStatus.QUEUED);
        states.assertTransition(JobStatus.QUEUED, JobStatus.RUNNING);
        states.assertTransition(JobStatus.RUNNING, JobStatus.DEAD_LETTER);
    }

    @Test
    void invalidTransitionsAreRejected() {
        assertThatThrownBy(() -> states.assertTransition(JobStatus.SUCCEEDED, JobStatus.RUNNING))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> states.assertTransition(JobStatus.DEAD_LETTER, JobStatus.QUEUED))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> states.assertTransition(JobStatus.CANCELLED, JobStatus.QUEUED))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> states.assertTransition(JobStatus.SCHEDULED, JobStatus.SUCCEEDED))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void cancelIsPossibleFromWaitingStates() {
        assertThatCode(() -> states.assertTransition(JobStatus.SCHEDULED, JobStatus.CANCELLED)).doesNotThrowAnyException();
        assertThatCode(() -> states.assertTransition(JobStatus.QUEUED, JobStatus.CANCELLED)).doesNotThrowAnyException();
        assertThatCode(() -> states.assertTransition(JobStatus.RETRY_WAIT, JobStatus.CANCELLED)).doesNotThrowAnyException();
        assertThatCode(() -> states.assertTransition(JobStatus.RUNNING, JobStatus.CANCELLED)).doesNotThrowAnyException();
    }
}
