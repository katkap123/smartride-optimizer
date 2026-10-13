
package com.smartride.simulation;

import java.util.Comparator;
import java.util.Objects;
import java.util.PriorityQueue;

import com.smartride.model.DispatchEvent;
import com.smartride.model.DispatchEventType;

public class DispatchEventQueue {

    private final PriorityQueue<DispatchEvent> events =
            new PriorityQueue<>(
                    Comparator.comparingLong(DispatchEvent::eventTimeSeconds)
                            .thenComparingInt(event ->
                                    eventPriority(event.eventType()))
                            .thenComparing(DispatchEvent::entityId)
            );

    public DispatchEvent peek() {
        return events.peek();
    }
    public void add(DispatchEvent event) {
        events.add(Objects.requireNonNull(event));
    }

    public DispatchEvent poll() {
        return events.poll();
    }

    public boolean isEmpty() {
        return events.isEmpty();
    }

    public int size() {
        return events.size();
    }

    private static int eventPriority(DispatchEventType type) {
        return switch (type) {
            case DRIVER_AVAILABLE -> 0;
            case RIDE_REQUESTED -> 1;
            case RIDE_EXPIRED -> 2;
        };
    }
}
