package com.taskflow.worker;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

record WorkerRegistrationRequest(@NotBlank String name, JsonNode metadata) {
}

record WorkerResponse(String name, WorkerStatus status, java.time.OffsetDateTime lastHeartbeatAt,
                             java.time.OffsetDateTime registeredAt) {
    public static WorkerResponse from(Worker worker) {
        return new WorkerResponse(worker.getName(), worker.getStatus(),
                worker.getLastHeartbeatAt(), worker.getRegisteredAt());
    }
}

@RestController
@RequestMapping("/api/workers")
@Tag(name = "Workers")
public class WorkerController {

    private final WorkerService workers;

    public WorkerController(WorkerService workers) {
        this.workers = workers;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a worker node (the app also registers its own embedded worker)")
    public WorkerResponse register(@RequestBody WorkerRegistrationRequest request) {
        return WorkerResponse.from(workers.register(request.name(), request.metadata()));
    }

    @PostMapping("/heartbeat")
    @Operation(summary = "Refresh a worker's heartbeat")
    public WorkerResponse heartbeat(@RequestBody WorkerRegistrationRequest request) {
        return WorkerResponse.from(workers.heartbeat(request.name()));
    }

    @GetMapping
    @Operation(summary = "List all workers with health status")
    public List<WorkerResponse> list() {
        return workers.findAll().stream().map(WorkerResponse::from).toList();
    }
}
