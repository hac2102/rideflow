package com.rideflow.core.simulation;

import com.rideflow.core.enums.SimulationStatus;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Deterministic discrete-event simulation clock.
 * Base configuration: 1 tick = 1 simulated minute.
 */
public class SimulationClock {

    private final LocalDateTime baseTime;
    private final AtomicLong currentTick;
    private volatile SimulationStatus status;

    public SimulationClock() {
        this(LocalDateTime.of(2026, 11, 19, 8, 0, 0)); // 08:00 AM scenario start
    }

    public SimulationClock(LocalDateTime baseTime) {
        this.baseTime = baseTime != null ? baseTime : LocalDateTime.now();
        this.currentTick = new AtomicLong(0);
        this.status = SimulationStatus.READY;
    }

    public long getTick() {
        return currentTick.get();
    }

    public LocalDateTime getCurrentSimulatedTime() {
        return baseTime.plusMinutes(currentTick.get());
    }

    public SimulationStatus getStatus() {
        return status;
    }

    public void start() {
        this.status = SimulationStatus.RUNNING;
    }

    public void pause() {
        this.status = SimulationStatus.PAUSED;
    }

    public void resume() {
        this.status = SimulationStatus.RUNNING;
    }

    public long step() {
        return currentTick.incrementAndGet();
    }

    public void reset() {
        currentTick.set(0);
        this.status = SimulationStatus.READY;
    }

    public void setTick(long tick) {
        currentTick.set(tick);
    }
}
