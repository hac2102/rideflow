package com.rideflow.core.dispatch;

import com.rideflow.core.enums.RequestType;
import com.rideflow.core.model.Coordinate;
import com.rideflow.core.model.RideRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class RideRequestComparatorTest {

    @Test
    @DisplayName("Priority request ranks ahead of standard request created at same time")
    void testPriorityTypeOrdering() {
        LocalDateTime now = LocalDateTime.now();
        RideRequestComparator comparator = new RideRequestComparator(now);

        RideRequest standardReq = new RideRequest();
        standardReq.setRequestId(1L);
        standardReq.setRequestType(RequestType.STANDARD);
        standardReq.setRequestedAt(now);

        RideRequest priorityReq = new RideRequest();
        priorityReq.setRequestId(2L);
        priorityReq.setRequestType(RequestType.PRIORITY);
        priorityReq.setRequestedAt(now);

        // priorityReq should rank higher (comparator returns negative when first argument has higher priority)
        assertTrue(comparator.compare(priorityReq, standardReq) < 0);
    }

    @Test
    @DisplayName("Standard request with significant wait time outranks newly arrived priority request due to aging")
    void testQueueAgingStarvationPrevention() {
        LocalDateTime now = LocalDateTime.now();
        RideRequestComparator comparator = new RideRequestComparator(now);

        // Standard request that has been waiting for 20 minutes (aging boost: 20 * 2 = 40 pts + 10 base = 50 pts)
        RideRequest waitingStandard = new RideRequest();
        waitingStandard.setRequestId(1L);
        waitingStandard.setRequestType(RequestType.STANDARD);
        waitingStandard.setRequestedAt(now.minusMinutes(20));

        // Newly arrived Priority request (0 wait time -> 20 pts base)
        RideRequest freshPriority = new RideRequest();
        freshPriority.setRequestId(2L);
        freshPriority.setRequestType(RequestType.PRIORITY);
        freshPriority.setRequestedAt(now);

        // waitingStandard should rank ahead of freshPriority, proving starvation resistance
        assertTrue(comparator.compare(waitingStandard, freshPriority) < 0);
    }
}
