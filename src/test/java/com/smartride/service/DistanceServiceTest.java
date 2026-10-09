package com.smartride.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.smartride.model.Location;

class DistanceServiceTest {

    private final DistanceService service =
            new DistanceService();

    @Test
    void sameLocationShouldReturnZero() {
        Location location = new Location(17.3850, 78.4867);

        assertEquals(
                0.0,
                service.calculateDistance(location, location),
                0.0001
        );
    }

    @Test
    void distanceShouldBeSymmetric() {
        Location a = new Location(17.3850, 78.4867);
        Location b = new Location(17.4401, 78.3489);

        assertEquals(
                service.calculateDistance(a, b),
                service.calculateDistance(b, a),
                0.0001
        );
    }

    @Test
    void distanceShouldBePositive() {
        Location a = new Location(17.3850, 78.4867);
        Location b = new Location(17.4401, 78.3489);

        assertTrue(service.calculateDistance(a, b) > 0);
    }

    @Test
    void nullLocationShouldThrowException() {
        Location a = new Location(17.3850, 78.4867);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.calculateDistance(a, null)
        );
    }
}