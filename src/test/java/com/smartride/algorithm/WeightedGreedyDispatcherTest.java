
package com.smartride.algorithm;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.smartride.model.DispatchResult;
import com.smartride.model.Driver;
import com.smartride.model.DriverRoute;
import com.smartride.model.Location;
import com.smartride.model.RideRequest;
import com.smartride.model.RideType;
import com.smartride.service.DistanceService;
import com.smartride.service.TravelTimeService;

class WeightedGreedyDispatcherTest {

    private final DistanceService distanceService =
            new DistanceService();

    private final TravelTimeService travelTimeService =
            new TravelTimeService(distanceService);

    @Test
    void shouldCalculateWeightedScore() {

        WeightedGreedyDispatcher dispatcher =
                new WeightedGreedyDispatcher(
                        distanceService,
                        travelTimeService,
                        1.0,
                        2.0
                );

        // 4 minutes waiting, 3 km empty driving.
        double score = dispatcher.calculateScore(
                240,
                3.0
        );

        assertEquals(10.0, score, 0.000001);
    }

    @Test
    void shouldRejectNegativeWeights() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new WeightedGreedyDispatcher(
                        distanceService,
                        travelTimeService,
                        -1.0,
                        2.0
                )
        );
    }

    @Test
    void shouldRejectBothWeightsZero() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new WeightedGreedyDispatcher(
                        distanceService,
                        travelTimeService,
                        0.0,
                        0.0
                )
        );
    }

    @Test
    void shouldRejectNegativeDistance() {

        WeightedGreedyDispatcher dispatcher =
                new WeightedGreedyDispatcher(
                        distanceService,
                        travelTimeService,
                        1.0,
                        2.0
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> dispatcher.calculateScore(
                        120,
                        -1.0
                )
        );
    }


    @Test
    void shouldAssignRideToDriverWithLowestWeightedScore() {

        Location pickup = new Location(17.3850, 78.4867);
        Location dropoff = new Location(17.3900, 78.4900);

        Driver closeButBusy = new Driver(
                "D1", pickup, 4, 600
        );

        Driver fartherButAvailable = new Driver(
                "D2",
                new Location(17.3950, 78.4967),
                4,
                0
        );

        RideRequest ride = new RideRequest(
                "R1", "U1", pickup, dropoff,
                RideType.SOLO, 1, 0, 1800, 0
        );

        WeightedGreedyDispatcher dispatcher =
                new WeightedGreedyDispatcher(
                        distanceService,
                        travelTimeService,
                        1.0,
                        2.0
                );

        DispatchResult result = dispatcher.optimize(
                List.of(closeButBusy, fartherButAvailable),
                List.of(ride)
        );

        DriverRoute selectedRoute = result.driverRoutes()
                .stream()
                .filter(route -> !route.stops().isEmpty())
                .findFirst()
                .orElseThrow();

        assertEquals("D2", selectedRoute.driverId());
        assertTrue(result.unassignedRides().isEmpty());
    }

}