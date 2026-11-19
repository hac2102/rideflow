package com.rideflow.core.dispatch;

import com.rideflow.core.model.RideRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;

/**
 * Manages the in-memory priority queue of queued ride requests with explicit aging.
 *
 * NOTE on Collections Design:
 * A standard Java PriorityQueue orders elements upon insertion or removal, but does NOT
 * detect changes in relative priority caused by the passage of time (aging).
 * To prevent starvation and ensure strictly ordered dispatch passes, RidePriorityQueue
 * supports deterministic queue rebuilding (reprioritization) at every simulation tick.
 */
public class RidePriorityQueue {

    private final Object lock = new Object();
    private PriorityQueue<RideRequest> queue;

    public RidePriorityQueue() {
        this.queue = new PriorityQueue<>(new RideRequestComparator(LocalDateTime.now()));
    }

    public void add(RideRequest request) {
        synchronized (lock) {
            queue.add(request);
        }
    }

    public Optional<RideRequest> poll() {
        synchronized (lock) {
            return Optional.ofNullable(queue.poll());
        }
    }

    public Optional<RideRequest> peek() {
        synchronized (lock) {
            return Optional.ofNullable(queue.peek());
        }
    }

    public boolean remove(RideRequest request) {
        synchronized (lock) {
            return queue.remove(request);
        }
    }

    public int size() {
        synchronized (lock) {
            return queue.size();
        }
    }

    public boolean isEmpty() {
        synchronized (lock) {
            return queue.isEmpty();
        }
    }

    public void clear() {
        synchronized (lock) {
            queue.clear();
        }
    }

    /**
     * Rebuilds the priority queue with updated aging scores based on the current simulation time.
     * Time complexity: O(N log N) to re-insert N requests.
     */
    public void reprioritize(LocalDateTime referenceTime) {
        synchronized (lock) {
            if (queue.isEmpty()) {
                return;
            }
            List<RideRequest> currentElements = new ArrayList<>(queue);
            PriorityQueue<RideRequest> newQueue = new PriorityQueue<>(new RideRequestComparator(referenceTime));
            newQueue.addAll(currentElements);
            this.queue = newQueue;
        }
    }

    /**
     * Returns an ordered snapshot of requests in priority order without mutating the queue.
     */
    public List<RideRequest> getOrderedSnapshot() {
        synchronized (lock) {
            List<RideRequest> copy = new ArrayList<>(queue);
            copy.sort(queue.comparator());
            return Collections.unmodifiableList(copy);
        }
    }
}
