package com.smartride.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.smartride.model.Location;

class TravelTimeServiceTest {

    private final TravelTimeService service =
            new TravelTimeService(new DistanceService());

    @Test
    void sameLocationShouldTakeZeroSeconds() {
        Location location = new Location(17.3850, 78.4867);

        assertEquals(
                0,
                service.estimateTravelTimeSeconds(
                        location, location
                )
        );
    }

    @Test
    void differentLocationsShouldTakePositiveTime() {
        Location a = new Location(17.3850, 78.4867);
        Location b = new Location(17.3900, 78.4900);

        assertTrue(
                service.estimateTravelTimeSeconds(a, b) > 0
        );
    }

    @Test
    void travelTimeShouldBeSymmetric() {
        Location a = new Location(17.3850, 78.4867);
        Location b = new Location(17.3900, 78.4900);

        assertEquals(
                service.estimateTravelTimeSeconds(a, b),
                service.estimateTravelTimeSeconds(b, a)
        );
    }
}