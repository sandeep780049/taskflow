package com.taskflow.job;

import com.taskflow.auth.JwtAuthFilter;
import com.taskflow.job.dto.CreateJobRequest;
import com.taskflow.job.dto.JobPageResponse;
import com.taskflow.job.dto.JobResponse;
import com.taskflow.job.dto.JobDetailResponse;
import com.taskflow.job.dto.JobPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@Tag(name = "Jobs")
public class JobController {

    private final JobService jobService;
    private final com.fasterxml.jackson.databind.ObjectMapper mapper;

    public JobController(JobService jobService, com.fasterxml.jackson.databind.ObjectMapper mapper) {
        this.jobService = jobService;
        this.mapper = mapper;
    }

    private UUID userId() {
        return JwtAuthFilter.currentUser().id();
    }

    private boolean isAdmin() {
        return JwtAuthFilter.currentUser().isAdmin();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a job (immediate, scheduled, or recurring)",
            description = "Immediate when scheduledAt is omitted. Provide recurringCron (6-field cron) for recurring jobs. " +
                    "Pass an idempotencyKey to guarantee the same logical job is not created twice.")
    public JobResponse create(@Valid @RequestBody CreateJobRequest request) {
        var job = jobService.create(userId(), request);
        return com.taskflow.job.dto.JobResponse.from(job, mapper);
    }

    @GetMapping
    @Operation(summary = "List jobs visible to the caller (own jobs, or all jobs for admins)")
    public JobPageResponse list(@RequestParam(required = false) String status,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "20") int size) {
        JobStatus parsed = status == null ? null : JobStatus.valueOf(status.trim().toUpperCase());
        return jobService.list(JwtAuthFilter.currentUser(), parsed, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one job with its executions and logs")
    public JobDetailResponse detail(@PathVariable UUID id) {
        return jobService.detail(userId(), isAdmin(), id);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a job (immediately if waiting, gracefully if running)")
    public java.util.Map<String, Object> cancel(@PathVariable UUID id) {
        boolean accepted = jobService.cancel(userId(), isAdmin(), id);
        return java.util.Map.of("cancelled", accepted);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a job (not allowed while running)")
    public void delete(@PathVariable UUID id) {
        jobService.delete(userId(), isAdmin(), id);
    }
}
