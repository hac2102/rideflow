package com.rideflow.core.dispatch;

import com.rideflow.core.model.RideRequest;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;

/**
 * Custom starvation-resistant comparator for ride dispatch prioritization.
 *
 * Factors considered:
 * 1. Request Type (PRIORITY vs STANDARD)
 * 2. Aging / Waiting Duration (prevents starvation of standard requests)
 * 3. Original Timestamp
 * 4. Request ID (deterministic tie-breaker)
 */
public class RideRequestComparator implements Comparator<RideRequest>, Serializable {
    private static final long serialVersionUID = 1L;

    private final LocalDateTime referenceTime;

    public RideRequestComparator() {
        this(LocalDateTime.now());
    }

    public RideRequestComparator(LocalDateTime referenceTime) {
        this.referenceTime = referenceTime != null ? referenceTime : LocalDateTime.now();
    }

    public int calculateEffectivePriority(RideRequest request) {
        if (request == null) return 0;
        int typeBoost = request.getRequestType().getPriorityWeight() * 10;
        long waitMinutes = 0;
        if (request.getRequestedAt() != null) {
            waitMinutes = Math.max(0, Duration.between(request.getRequestedAt(), referenceTime).toMinutes());
        }
        // Aging factor: 2 points per minute of waiting
        return typeBoost + (int) (waitMinutes * 2);
    }

    @Override
    public int compare(RideRequest r1, RideRequest r2) {
        if (r1 == r2) return 0;
        if (r1 == null) return 1;
        if (r2 == null) return -1;

        // 1. Compare effective priority (higher priority first)
        int p1 = calculateEffectivePriority(r1);
        int p2 = calculateEffectivePriority(r2);
        int priorityCompare = Integer.compare(p2, p1);
        if (priorityCompare != 0) {
            return priorityCompare;
        }

        // 2. Earlier request timestamp first
        if (r1.getRequestedAt() != null && r2.getRequestedAt() != null) {
            int timeCompare = r1.getRequestedAt().compareTo(r2.getRequestedAt());
            if (timeCompare != 0) {
                return timeCompare;
            }
        }

        // 3. Deterministic tie-breaker: smaller requestId first
        if (r1.getRequestId() != null && r2.getRequestId() != null) {
            return Long.compare(r1.getRequestId(), r2.getRequestId());
        }

        return 0;
    }
}
