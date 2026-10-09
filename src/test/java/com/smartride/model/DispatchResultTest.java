package com.smartride.model;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DispatchResultTest {

    @Test
    void shouldCreateEmptyDispatchResult() {
        DispatchResult result = new DispatchResult(
                List.of(),
                List.of(),
                0.0,
                0L
        );

        assertTrue(result.driverRoutes().isEmpty());
        assertTrue(result.unassignedRides().isEmpty());
        assertEquals(0.0, result.totalDistanceKm());
        assertEquals(0L, result.totalRiderWaitSeconds());
    }

    @Test
    void shouldRejectNegativeWaitTime() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DispatchResult(
                        List.of(),
                        List.of(),
                        0.0,
                        -1L
                )
        );
    }
}