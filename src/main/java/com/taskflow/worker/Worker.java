package com.taskflow.worker;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "workers")
public class Worker {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkerStatus status = WorkerStatus.ACTIVE;

    @Column(name = "last_heartbeat_at", nullable = false)
    private OffsetDateTime lastHeartbeatAt = OffsetDateTime.now();

    @Column(name = "registered_at", nullable = false)
    private OffsetDateTime registeredAt = OffsetDateTime.now();

    @Column(columnDefinition = "jsonb")
    private String metadata;

    public Worker() {
    }

    public Worker(String name, String metadata) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.metadata = metadata;
    }

    public void refreshHeartbeat() {
        this.lastHeartbeatAt = OffsetDateTime.now();
        this.status = WorkerStatus.ACTIVE;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public WorkerStatus getStatus() { return status; }
    public void setStatus(WorkerStatus status) { this.status = status; }
    public OffsetDateTime getLastHeartbeatAt() { return lastHeartbeatAt; }
    public void setLastHeartbeatAt(OffsetDateTime t) { this.lastHeartbeatAt = t; }
    public OffsetDateTime getRegisteredAt() { return registeredAt; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}
