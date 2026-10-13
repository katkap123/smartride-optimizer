
package com.smartride.algorithm;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.smartride.model.DispatchResult;
import com.smartride.model.Driver;
import com.smartride.model.DriverRoute;
import com.smartride.model.Location;
import com.smartride.model.RideRequest;
import com.smartride.model.RideType;
import com.smartride.model.RouteStop;
import com.smartride.model.StopType;
import com.smartride.service.DistanceService;
import com.smartride.service.TravelTimeService;

class TimeAwareDispatcherTest {

    @Test
    void shouldRejectDuplicateDriverIds() {

        DistanceService distanceService = new DistanceService();
        TravelTimeService travelTimeService =
                new TravelTimeService(distanceService);

        TimeAwareDispatcher dispatcher =
                new TimeAwareDispatcher(
                        distanceService,
                        travelTimeService
                );

        Driver driver1 = new Driver(
                "D1",
                new Location(17.3850, 78.4867),
                4,
                0
        );

        Driver driver2 = new Driver(
                "D1",
                new Location(17.3900, 78.4900),
                4,
                0
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> dispatcher.optimize(
                        List.of(driver1, driver2),
                        List.of()
                )
        );
    }
    @Test
    void shouldMarkExpiredRideAsUnassigned() {

        DistanceService distanceService = new DistanceService();
        TravelTimeService travelTimeService =
                new TravelTimeService(distanceService);

        TimeAwareDispatcher dispatcher =
                new TimeAwareDispatcher(distanceService, travelTimeService);

        RideRequest ride = new RideRequest(
                "R1",
                "Rider1",
                new Location(17.3850, 78.4867),
                new Location(17.3900, 78.4900),
                RideType.SOLO,
                1,
                0,
                300,
                0
        );

        DispatchResult result = dispatcher.optimize(
                List.of(),
                List.of(ride)
        );

        assertEquals(1, result.unassignedRides().size());
        assertEquals("R1", result.unassignedRides().get(0).id());
        assertEquals(0, result.totalDistanceKm());
    }


    @Test
    void shouldAssignRideToAvailableDriver() {

        DistanceService distanceService = new DistanceService();
        TravelTimeService travelTimeService =
                new TravelTimeService(distanceService);

        TimeAwareDispatcher dispatcher =
                new TimeAwareDispatcher(distanceService, travelTimeService);

        Location pickup = new Location(17.3850, 78.4867);
        Location dropoff = new Location(17.3900, 78.4900);

        Driver driver = new Driver("D1", pickup, 4, 0);

        RideRequest ride = new RideRequest(
                "R1",
                "Rider1",
                pickup,
                dropoff,
                RideType.SOLO,
                1,
                120,
                300,
                0
        );

        DispatchResult result = dispatcher.optimize(
                List.of(driver),
                List.of(ride)
        );

        assertTrue(result.unassignedRides().isEmpty());

        DriverRoute route = result.driverRoutes().get(0);

        assertEquals(2, route.stops().size());
        assertEquals("R1", route.stops().get(0).rideId());
        assertEquals(StopType.PICKUP, route.stops().get(0).type());
        assertEquals(120, route.stops().get(0).arrivalAtSeconds());

        assertEquals(StopType.DROPOFF, route.stops().get(1).type());
        assertEquals(0, result.totalRiderWaitSeconds());
    }


@Test
void shouldAssignRideWhenDriverBecomesAvailableLater() {

    DistanceService distanceService = new DistanceService();
    TravelTimeService travelTimeService =
            new TravelTimeService(distanceService);

    TimeAwareDispatcher dispatcher =
            new TimeAwareDispatcher(distanceService, travelTimeService);

    Location pickup = new Location(17.3850, 78.4867);
    Location dropoff = new Location(17.3900, 78.4900);

    Driver driver = new Driver(
            "D1",
            pickup,
            4,
            300
    );

    RideRequest ride = new RideRequest(
            "R1",
            "Rider1",
            pickup,
            dropoff,
            RideType.SOLO,
            1,
            0,
            600,
            0
    );

    DispatchResult result = dispatcher.optimize(
            List.of(driver),
            List.of(ride)
    );

    assertTrue(result.unassignedRides().isEmpty());

    DriverRoute route = result.driverRoutes().get(0);

    assertEquals(2, route.stops().size());

    RouteStop pickupStop = route.stops().get(0);

    assertEquals("R1", pickupStop.rideId());
    assertEquals(StopType.PICKUP, pickupStop.type());
    assertEquals(300, pickupStop.arrivalAtSeconds());

    assertEquals(300, result.totalRiderWaitSeconds());
}



@Test
void shouldRejectRideWhenPickupExceedsDeadline() {

    DistanceService distanceService = new DistanceService();
    TravelTimeService travelTimeService =
            new TravelTimeService(distanceService);

    TimeAwareDispatcher dispatcher =
            new TimeAwareDispatcher(distanceService, travelTimeService);

    Location driverLocation = new Location(17.3850, 78.4867);
    Location pickup = new Location(17.4050, 78.5067);
    Location dropoff = new Location(17.4100, 78.5100);

    Driver driver = new Driver(
            "D1",
            driverLocation,
            4,
            300
    );

    RideRequest ride = new RideRequest(
            "R1",
            "Rider1",
            pickup,
            dropoff,
            RideType.SOLO,
            1,
            0,
            400,
            0
    );

    DispatchResult result = dispatcher.optimize(
            List.of(driver),
            List.of(ride)
    );

    assertEquals(1, result.unassignedRides().size());
    assertEquals("R1", result.unassignedRides().get(0).id());

    DriverRoute route = result.driverRoutes().get(0);

    assertTrue(route.stops().isEmpty());
    assertEquals(0, result.totalRiderWaitSeconds());
    assertEquals(0.0, result.totalDistanceKm(), 0.0001);
}


@Test
void shouldSelectDriverWithEarliestPickupTime() {

    DistanceService distanceService = new DistanceService();
    TravelTimeService travelTimeService =
            new TravelTimeService(distanceService);

    TimeAwareDispatcher dispatcher =
            new TimeAwareDispatcher(distanceService, travelTimeService);

    Location pickup = new Location(17.3850, 78.4867);
    Location dropoff = new Location(17.3900, 78.4900);
    Location farAway = new Location(17.4050, 78.5067);

    Driver driver1 = new Driver("D1", farAway, 4, 0);
    Driver driver2 = new Driver("D2", pickup, 4, 0);

    RideRequest ride = new RideRequest(
            "R1",
            "Rider1",
            pickup,
            dropoff,
            RideType.SOLO,
            1,
            0,
            900,
            0
    );

    DispatchResult result = dispatcher.optimize(
            List.of(driver1, driver2),
            List.of(ride)
    );

    assertTrue(result.unassignedRides().isEmpty());

    DriverRoute route1 = result.driverRoutes().stream()
            .filter(route -> route.driverId().equals("D1"))
            .findFirst()
            .orElseThrow();

    DriverRoute route2 = result.driverRoutes().stream()
            .filter(route -> route.driverId().equals("D2"))
            .findFirst()
            .orElseThrow();

    assertTrue(route1.stops().isEmpty());
    assertEquals(2, route2.stops().size());

    assertEquals("R1", route2.stops().get(0).rideId());
    assertEquals(0, route2.stops().get(0).arrivalAtSeconds());
    assertEquals(0, result.totalRiderWaitSeconds());
}

@Test
void shouldSelectDriverByIdWhenPickupTimesAreEqual() {

    DistanceService distanceService = new DistanceService();
    TravelTimeService travelTimeService =
            new TravelTimeService(distanceService);

    TimeAwareDispatcher dispatcher =
            new TimeAwareDispatcher(distanceService, travelTimeService);

    Location pickup = new Location(17.3850, 78.4867);
    Location dropoff = new Location(17.3900, 78.4900);

    Driver driver2 = new Driver("D2", pickup, 4, 0);
    Driver driver1 = new Driver("D1", pickup, 4, 0);

    RideRequest ride = new RideRequest(
            "R1",
            "Rider1",
            pickup,
            dropoff,
            RideType.SOLO,
            1,
            0,
            900,
            0
    );

    DispatchResult result = dispatcher.optimize(
            List.of(driver2, driver1),
            List.of(ride)
    );

    DriverRoute route1 = result.driverRoutes().stream()
            .filter(route -> route.driverId().equals("D1"))
            .findFirst()
            .orElseThrow();

    DriverRoute route2 = result.driverRoutes().stream()
            .filter(route -> route.driverId().equals("D2"))
            .findFirst()
            .orElseThrow();

    assertEquals(2, route1.stops().size());
    assertTrue(route2.stops().isEmpty());
}


@Test
void shouldRespectDriverCapacity() {

    DistanceService distanceService = new DistanceService();
    TravelTimeService travelTimeService =
            new TravelTimeService(distanceService);

    TimeAwareDispatcher dispatcher =
            new TimeAwareDispatcher(distanceService, travelTimeService);

    Location pickup = new Location(17.3850, 78.4867);
    Location dropoff = new Location(17.3900, 78.4900);

    Driver driver1 = new Driver("D1", pickup, 2, 0);
    Driver driver2 = new Driver("D2", pickup, 4, 0);

    RideRequest ride = new RideRequest(
            "R1",
            "Rider1",
            pickup,
            dropoff,
            RideType.SOLO,
            3,
            0,
            900,
            0
    );

    DispatchResult result = dispatcher.optimize(
            List.of(driver1, driver2),
            List.of(ride)
    );

    assertTrue(result.unassignedRides().isEmpty());

    DriverRoute route1 = result.driverRoutes().stream()
            .filter(route -> route.driverId().equals("D1"))
            .findFirst()
            .orElseThrow();

    DriverRoute route2 = result.driverRoutes().stream()
            .filter(route -> route.driverId().equals("D2"))
            .findFirst()
            .orElseThrow();

    assertTrue(route1.stops().isEmpty());
    assertEquals(2, route2.stops().size());

    assertEquals(3, route2.stops().get(0).occupancyAfterStop());
    assertEquals(0, route2.stops().get(1).occupancyAfterStop());
}


@Test
void shouldNotLetFutureRideAffectEarlierAssignment() {

    DistanceService distanceService = new DistanceService();
    TravelTimeService travelTimeService =
            new TravelTimeService(distanceService);

    TimeAwareDispatcher dispatcher =
            new TimeAwareDispatcher(distanceService, travelTimeService);

    Location a = new Location(17.3850, 78.4867);
    Location b = new Location(17.3900, 78.4900);
    Location c = new Location(17.3950, 78.4950);

    Driver driver = new Driver("D1", a, 4, 0);

    RideRequest ride1 = new RideRequest(
            "R1", "Rider1", a, b,
            RideType.SOLO, 1,
            0, 900, 0
    );

    RideRequest ride2 = new RideRequest(
            "R2", "Rider2", b, c,
            RideType.SOLO, 1,
            600, 900, 0
    );

    DispatchResult resultWithOnlyR1 = dispatcher.optimize(
            List.of(driver),
            List.of(ride1)
    );

    DispatchResult resultWithBothRides = dispatcher.optimize(
            List.of(driver),
            List.of(ride1, ride2)
    );

    RouteStop firstPickupWithoutR2 =
            resultWithOnlyR1.driverRoutes().get(0).stops().get(0);

    RouteStop firstPickupWithR2 =
            resultWithBothRides.driverRoutes().get(0).stops().get(0);

    assertEquals("R1", firstPickupWithR2.rideId());

    assertEquals(
            firstPickupWithoutR2.arrivalAtSeconds(),
            firstPickupWithR2.arrivalAtSeconds()
    );

    assertEquals(
            firstPickupWithoutR2.location(),
            firstPickupWithR2.location()
    );
}


@Test
void shouldRejectInvalidScoringWeights() {

    DistanceService distanceService = new DistanceService();
    TravelTimeService travelTimeService =
            new TravelTimeService(distanceService);

    assertThrows(
            IllegalArgumentException.class,
            () -> new TimeAwareDispatcher(
                    distanceService,
                    travelTimeService,
                    -1.0,
                    2.0
            )
    );

    assertThrows(
            IllegalArgumentException.class,
            () -> new TimeAwareDispatcher(
                    distanceService,
                    travelTimeService,
                    0.0,
                    0.0
            )
    );
}


@Test
void shouldPreferLowerEmptyDistanceWhenWeighted() {

    DistanceService distanceService = new DistanceService();
    TravelTimeService travelTimeService =
            new TravelTimeService(distanceService);

    Location pickup = new Location(17.3850, 78.4867);
    Location dropoff = new Location(17.3900, 78.4900);
    Location fartherLocation = new Location(17.4000, 78.5000);

    Driver fartherDriver = new Driver(
            "D1", fartherLocation, 4, 0
    );

    Driver closerDriver = new Driver(
            "D2", pickup, 4, 0
    );

    RideRequest ride = new RideRequest(
            "R1", "Rider1",
            pickup, dropoff,
            RideType.SOLO, 1,
            0, 900, 0
    );

    TimeAwareDispatcher dispatcher =
            new TimeAwareDispatcher(
                    distanceService,
                    travelTimeService,
                    1.0,
                    3.0
            );

    DispatchResult result = dispatcher.optimize(
            List.of(fartherDriver, closerDriver),
            List.of(ride)
    );

    DriverRoute selectedRoute = result.driverRoutes().stream()
            .filter(route -> !route.stops().isEmpty())
            .findFirst()
            .orElseThrow();

    assertEquals("D2", selectedRoute.driverId());
}

@Test
void shouldAssignSimultaneousRidesToNearbyDrivers() {

    DistanceService distanceService = new DistanceService();
    TravelTimeService travelTimeService =
            new TravelTimeService(distanceService);

    TimeAwareDispatcher dispatcher =
            new TimeAwareDispatcher(distanceService, travelTimeService);

    Location a = new Location(17.3850, 78.4867);
    Location b = new Location(17.4050, 78.5067);

    Driver d1 = new Driver("D1", a, 4, 0);
    Driver d2 = new Driver("D2", b, 4, 0);

    RideRequest r1 = new RideRequest(
            "R1", "Rider1",
            a, new Location(17.3900, 78.4900),
            RideType.SOLO, 1,
            0, 900, 0
    );

    RideRequest r2 = new RideRequest(
            "R2", "Rider2",
            b, new Location(17.4100, 78.5100),
            RideType.SOLO, 1,
            0, 900, 0
    );

    DispatchResult result = dispatcher.optimize(
            List.of(d1, d2),
            List.of(r1, r2)
    );

    DriverRoute route1 = result.driverRoutes().stream()
            .filter(route -> route.driverId().equals("D1"))
            .findFirst()
            .orElseThrow();

    DriverRoute route2 = result.driverRoutes().stream()
            .filter(route -> route.driverId().equals("D2"))
            .findFirst()
            .orElseThrow();

    assertEquals("R1", route1.stops().get(0).rideId());
    assertEquals("R2", route2.stops().get(0).rideId());

    assertEquals(0.0, route1.emptyDistanceKm(), 0.0001);
    assertEquals(0.0, route2.emptyDistanceKm(), 0.0001);
}


@Test
void shouldSelectBestPairAcrossPendingRides() {

    DistanceService distanceService = new DistanceService();
    TravelTimeService travelTimeService =
            new TravelTimeService(distanceService);

    TimeAwareDispatcher dispatcher =
            new TimeAwareDispatcher(
                    distanceService,
                    travelTimeService
            );

    Location driverLocation =
            new Location(17.3850, 78.4867);

    Location farPickup =
            new Location(17.4050, 78.5067);

    Location nearbyDropoff =
            new Location(17.3900, 78.4900);

    Driver driver = new Driver(
            "D1",
            driverLocation,
            4,
            0
    );

    // R1 comes first alphabetically but is farther away.
    RideRequest ride1 = new RideRequest(
            "R1",
            "Rider1",
            farPickup,
            nearbyDropoff,
            RideType.SOLO,
            1,
            0,
            1800,
            0
    );

    // R2 is at the driver's current location.
    RideRequest ride2 = new RideRequest(
            "R2",
            "Rider2",
            driverLocation,
            nearbyDropoff,
            RideType.SOLO,
            1,
            0,
            1800,
            0
    );

    DispatchResult result = dispatcher.optimize(
            List.of(driver),
            List.of(ride1, ride2)
    );

    DriverRoute route = result.driverRoutes().get(0);

    assertFalse(route.stops().isEmpty());

    // R2 should be selected first because its pickup
    // requires no empty driving.
    assertEquals(
            "R2",
            route.stops().get(0).rideId()
    );

    assertEquals(
            StopType.PICKUP,
            route.stops().get(0).type()
    );
}

@Test
void shouldMaintainOptimizedDispatchMetrics() {

    Location a = new Location(17.3850, 78.4867);
    Location b = new Location(17.3900, 78.4900);
    Location c = new Location(17.4000, 78.4500);
    Location d = new Location(17.4050, 78.4550);
    Location e = new Location(17.4401, 78.3489);
    Location f = new Location(17.4450, 78.3550);
    DistanceService distanceService = new DistanceService();

    TravelTimeService travelTimeService =
            new TravelTimeService(distanceService);

    TimeAwareDispatcher dispatcher =
            new TimeAwareDispatcher(
                    distanceService,
                    travelTimeService
            );

    List<Driver> drivers = List.of(
                new Driver("D1", a, 4, 600),
                new Driver("D2", new Location(17.3950, 78.4967), 4, 0),
                new Driver("D3", e, 3, 300)
        );

    List<RideRequest> rides = List.of(
                new RideRequest(
                        "R1", "U1", a, b,
                        RideType.SOLO, 1, 0, 900, 0
                ),
                new RideRequest(
                        "R2", "U2", b, c,
                        RideType.SOLO, 1, 120, 900, 0
                ),
                new RideRequest(
                        "R3", "U3", c, d,
                        RideType.SHARED, 1, 240, 900, 600
                ),
                new RideRequest(
                        "R4", "U4", d, a,
                        RideType.SOLO, 1, 360, 900, 0
                ),
                new RideRequest(
                        "R5", "U5", e, f,
                        RideType.SHARED, 1, 60, 900, 600
                ),
                new RideRequest(
                        "R6", "U6", f, e,
                        RideType.SOLO, 1, 480, 900, 0
                )
        );

    DispatchResult result =
            dispatcher.optimize(drivers, rides);

    double emptyDistanceKm = result.driverRoutes()
            .stream()
            .mapToDouble(DriverRoute::emptyDistanceKm)
            .sum();

    assertEquals(0, result.unassignedRides().size());

    assertEquals(17.11, result.totalDistanceKm(), 0.02);
    assertEquals(5.57, emptyDistanceKm, 0.02);

    assertEquals(
            30.73,
            result.totalRiderWaitSeconds() / 60.0,
            0.05
    );
}

}
