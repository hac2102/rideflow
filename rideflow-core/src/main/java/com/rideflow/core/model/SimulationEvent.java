package com.rideflow.core.model;

import com.rideflow.core.enums.EventType;
import java.io.Serializable;
import java.time.LocalDateTime;

public class SimulationEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long eventId;
    private Long runId;
    private long simulationTick;
    private EventType eventType;
    private Long entityId;
    private String payloadJson;
    private LocalDateTime createdAt;

    public SimulationEvent() {
        this.createdAt = LocalDateTime.now();
    }

    public SimulationEvent(Long eventId, Long runId, long simulationTick, EventType eventType,
                           Long entityId, String payloadJson) {
        this.eventId = eventId;
        this.runId = runId;
        this.simulationTick = simulationTick;
        this.eventType = eventType;
        this.entityId = entityId;
        this.payloadJson = payloadJson;
        this.createdAt = LocalDateTime.now();
    }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public Long getRunId() { return runId; }
    public void setRunId(Long runId) { this.runId = runId; }

    public long getSimulationTick() { return simulationTick; }
    public void setSimulationTick(long simulationTick) { this.simulationTick = simulationTick; }

    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "SimulationEvent{" +
                "tick=" + simulationTick +
                ", type=" + eventType +
                ", entityId=" + entityId +
                ", payload='" + payloadJson + '\'' +
                '}';
    }
}
