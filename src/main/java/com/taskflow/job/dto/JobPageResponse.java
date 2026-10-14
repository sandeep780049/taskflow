package com.taskflow.job.dto;

import com.taskflow.job.Job;

public record JobPageResponse(long page, long totalPages, long totalElements, java.util.List<JobResponse> items) {
}
