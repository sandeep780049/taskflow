package com.taskflow.worker;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class WorkerService {

    private final WorkerRepository workers;

    public WorkerService(WorkerRepository workers) {
        this.workers = workers;
    }

    @Transactional
    public Worker register(String name, JsonNode metadata) {
        Worker worker = workers.findByName(name).orElseGet(() -> new Worker(name,
                metadata == null ? null : metadata.toString()));
        if (metadata != null) {
            worker.setMetadata(metadata.toString());
        }
        worker.refreshHeartbeat();
        return workers.save(worker);
    }

    @Transactional
    public Worker heartbeat(String name) {
        Worker worker = workers.findByName(name)
                .orElseThrow(() -> com.taskflow.common.ApiException.notFound("Worker not registered: " + name));
        worker.refreshHeartbeat();
        return workers.save(worker);
    }

    @Transactional(readOnly = true)
    public List<Worker> findAll() {
        return workers.findAll();
    }

    @Transactional
    public int markStaleWorkers(int staleSeconds) {
        var cutoff = OffsetDateTime.now().minusSeconds(staleSeconds);
        List<Worker> stale = workers.findByStatusAndLastHeartbeatAtBefore(WorkerStatus.ACTIVE, cutoff);
        stale.forEach(w -> w.setStatus(WorkerStatus.STALE));
        workers.saveAll(stale);
        return stale.size();
    }
}
