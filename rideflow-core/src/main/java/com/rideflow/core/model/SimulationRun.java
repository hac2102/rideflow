package com.rideflow.core.model;

import com.rideflow.core.enums.SimulationStatus;
import java.io.Serializable;
import java.time.LocalDateTime;

public class SimulationRun implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long runId;
    private String scenarioName;
    private long randomSeed;
    private long currentTick;
    private SimulationStatus status;
    private int totalRequests;
    private int completedRequests;
    private int cancelledRequests;
    private int expiredRequests;
    private Money totalRevenue;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    public SimulationRun() {
        this.status = SimulationStatus.READY;
        this.currentTick = 0;
        this.totalRequests = 0;
        this.completedRequests = 0;
        this.cancelledRequests = 0;
        this.expiredRequests = 0;
        this.totalRevenue = Money.ZERO;
        this.startedAt = LocalDateTime.now();
    }

    public SimulationRun(Long runId, String scenarioName, long randomSeed) {
        this();
        this.runId = runId;
        this.scenarioName = scenarioName;
        this.randomSeed = randomSeed;
    }

    public Long getRunId() { return runId; }
    public void setRunId(Long runId) { this.runId = runId; }

    public String getScenarioName() { return scenarioName; }
    public void setScenarioName(String scenarioName) { this.scenarioName = scenarioName; }

    public long getRandomSeed() { return randomSeed; }
    public void setRandomSeed(long randomSeed) { this.randomSeed = randomSeed; }

    public long getCurrentTick() { return currentTick; }
    public void setCurrentTick(long currentTick) { this.currentTick = currentTick; }

    public SimulationStatus getStatus() { return status; }
    public void setStatus(SimulationStatus status) { this.status = status; }

    public int getTotalRequests() { return totalRequests; }
    public void setTotalRequests(int totalRequests) { this.totalRequests = totalRequests; }

    public int getCompletedRequests() { return completedRequests; }
    public void setCompletedRequests(int completedRequests) { this.completedRequests = completedRequests; }

    public int getCancelledRequests() { return cancelledRequests; }
    public void setCancelledRequests(int cancelledRequests) { this.cancelledRequests = cancelledRequests; }

    public int getExpiredRequests() { return expiredRequests; }
    public void setExpiredRequests(int expiredRequests) { this.expiredRequests = expiredRequests; }

    public Money getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(Money totalRevenue) { this.totalRevenue = totalRevenue; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }
}
