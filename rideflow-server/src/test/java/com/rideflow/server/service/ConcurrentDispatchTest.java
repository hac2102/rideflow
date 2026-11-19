package com.rideflow.server.service;

import com.rideflow.core.enums.DriverStatus;
import com.rideflow.core.enums.RequestType;
import com.rideflow.core.enums.VehicleType;
import com.rideflow.core.exception.DoubleBookingException;
import com.rideflow.core.model.Coordinate;
import com.rideflow.core.model.Driver;
import com.rideflow.core.model.RideRequest;
import com.rideflow.server.RideFlowServerApplication;
import com.rideflow.server.dao.DriverDao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = RideFlowServerApplication.class)
class ConcurrentDispatchTest {

    @Autowired
    private DispatchService dispatchService;

    @Autowired
    private RideLifecycleService rideLifecycleService;

    @Autowired
    private DriverDao driverDao;

    @Test
    @DisplayName("Mandatory Concurrency Test: Two competing threads assigning same driver - exactly ONE succeeds")
    void testAntiDoubleBookingUnderConcurrentDispatch() throws Exception {
        // 1. Create a single dedicated available driver
        Driver driver = new Driver(
                null, "Contested Driver", VehicleType.SEDAN, "KA-01-RACE-" + System.currentTimeMillis(),
                Coordinate.of(50.0, 50.0), 13
        );
        long driverId = driverDao.save(driver);

        // 2. Create two competing ride requests
        RideRequest ride1 = rideLifecycleService.createAndQueueRide(
                1L, Coordinate.of(50.5, 50.5), Coordinate.of(60.0, 60.0), RequestType.STANDARD, 1L
        );
        RideRequest ride2 = rideLifecycleService.createAndQueueRide(
                2L, Coordinate.of(50.2, 50.2), Coordinate.of(70.0, 70.0), RequestType.PRIORITY, 1L
        );

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger doubleBookingPreventedCount = new AtomicInteger(0);

        // Thread A attempts to assign Driver to Ride 1
        executor.submit(() -> {
            try {
                barrier.await();
                dispatchService.atomicAssignRide(ride1.getRequestId(), driverId, 1L, 1L);
                successCount.incrementAndGet();
            } catch (DoubleBookingException e) {
                doubleBookingPreventedCount.incrementAndGet();
            } catch (Exception ignored) {
            } finally {
                latch.countDown();
            }
        });

        // Thread B attempts to assign same Driver to Ride 2 simultaneously
        executor.submit(() -> {
            try {
                barrier.await();
                dispatchService.atomicAssignRide(ride2.getRequestId(), driverId, 1L, 1L);
                successCount.incrementAndGet();
            } catch (DoubleBookingException e) {
                doubleBookingPreventedCount.incrementAndGet();
            } catch (Exception ignored) {
            } finally {
                latch.countDown();
            }
        });

        boolean finishedInTime = latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finishedInTime, "Concurrent assignment timed out");

        // PROOF OF INVARIANT:
        // Exactly ONE transaction committed; the other caught DoubleBookingException and rolled back.
        assertEquals(1, successCount.get(), "Exactly one assignment must succeed!");
        assertEquals(1, doubleBookingPreventedCount.get(), "Competing thread must be prevented from double booking!");

        // Invariant: driver status in database must be RESERVED (never assigned twice)
        Driver finalDriver = driverDao.findById(driverId).orElseThrow();
        assertEquals(DriverStatus.RESERVED, finalDriver.getStatus());
    }
}
