
package com.smartride.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.smartride.model.DispatchEvent;
import com.smartride.model.DispatchEventType;

class DispatchEventQueueTest {

    @Test
    void shouldProcessEventsChronologically() {

        DispatchEventQueue queue = new DispatchEventQueue();

        queue.add(new DispatchEvent(
                240, DispatchEventType.RIDE_REQUESTED, "R3"
        ));

        queue.add(new DispatchEvent(
                0, DispatchEventType.RIDE_REQUESTED, "R1"
        ));

        queue.add(new DispatchEvent(
                120, DispatchEventType.RIDE_REQUESTED, "R2"
        ));

        assertEquals("R1", queue.poll().entityId());
        assertEquals("R2", queue.poll().entityId());
        assertEquals("R3", queue.poll().entityId());

        assertTrue(queue.isEmpty());
    }

    @Test
    void shouldPrioritizeDriverAvailabilityAtSameTime() {

        DispatchEventQueue queue = new DispatchEventQueue();

        queue.add(new DispatchEvent(
                120, DispatchEventType.RIDE_REQUESTED, "R1"
        ));

        queue.add(new DispatchEvent(
                120, DispatchEventType.DRIVER_AVAILABLE, "D1"
        ));

        assertEquals(
                DispatchEventType.DRIVER_AVAILABLE,
                queue.poll().eventType()
        );

        assertEquals(
                DispatchEventType.RIDE_REQUESTED,
                queue.poll().eventType()
        );
    }

    @Test
    void shouldOrderSameTimeAndTypeByEntityId() {

        DispatchEventQueue queue = new DispatchEventQueue();

        queue.add(new DispatchEvent(
                100, DispatchEventType.RIDE_REQUESTED, "R2"
        ));

        queue.add(new DispatchEvent(
                100, DispatchEventType.RIDE_REQUESTED, "R1"
        ));

        assertEquals("R1", queue.poll().entityId());
        assertEquals("R2", queue.poll().entityId());
    }

    @Test
    void shouldReturnNullWhenQueueIsEmpty() {

        DispatchEventQueue queue = new DispatchEventQueue();

        assertNull(queue.poll());
        assertEquals(0, queue.size());
    }
}
