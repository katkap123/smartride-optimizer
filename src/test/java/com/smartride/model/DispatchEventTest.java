package com.smartride.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class DispatchEventTest {

    @Test
    void shouldCreateValidEvent() {
        DispatchEvent event = new DispatchEvent(
                120,
                DispatchEventType.RIDE_REQUESTED,
                "R2"
        );

        assertEquals(120, event.eventTimeSeconds());
        assertEquals(DispatchEventType.RIDE_REQUESTED, event.eventType());
        assertEquals("R2", event.entityId());
    }

    @Test
    void shouldRejectNegativeEventTime() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DispatchEvent(
                        -1,
                        DispatchEventType.RIDE_REQUESTED,
                        "R1"
                )
        );
    }

    @Test
    void shouldRejectBlankEntityId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DispatchEvent(
                        100,
                        DispatchEventType.DRIVER_AVAILABLE,
                        " "
                )
        );
    }
}