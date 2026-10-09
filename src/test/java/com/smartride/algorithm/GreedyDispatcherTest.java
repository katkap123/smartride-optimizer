
package com.smartride.algorithm;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.smartride.model.DispatchResult;
import com.smartride.model.Driver;
import com.smartride.model.Location;
import com.smartride.model.RideRequest;
import com.smartride.model.RideType;
import com.smartride.model.RouteStop;
import com.smartride.model.StopType;
import com.smartride.service.DistanceService;
import com.smartride.service.TravelTimeService;

class GreedyDispatcherTest {

    private final DistanceService distanceService =
            new DistanceService();

    private final TravelTimeService travelTimeService =
            new TravelTimeService(distanceService);

    private final GreedyDispatcher dispatcher =
            new GreedyDispatcher(distanceService, travelTimeService);

    @Test
    void shouldAssignMultipleRidesToOneDriver() {

        Location a = new Location(17.3850, 78.4867);
        Location b = new Location(17.3900, 78.4900);
        Location c = new Location(17.3950, 78.4950);

        Driver driver = new Driver("D1", a, 4, 0);

        RideRequest ride1 = new RideRequest(
                "R1", "U1", a, b,
                RideType.SOLO, 1, 0, 3600, 0
        );

        RideRequest ride2 = new RideRequest(
                "R2", "U2", b, c,
                RideType.SHARED, 1, 0, 3600, 600
        );

        DispatchResult result = dispatcher.optimize(
                List.of(driver),
                List.of(ride1, ride2)
        );

        assertTrue(result.unassignedRides().isEmpty());
        assertEquals(1, result.driverRoutes().size());
        assertEquals(4, result.driverRoutes().get(0).stops().size());

        List<RouteStop> stops = result.driverRoutes().get(0).stops();

        assertEquals(StopType.PICKUP, stops.get(0).type());
        assertEquals(StopType.DROPOFF, stops.get(1).type());
        assertEquals(StopType.PICKUP, stops.get(2).type());
        assertEquals(StopType.DROPOFF, stops.get(3).type());
    }

    @Test
    void shouldChooseCloserDriverWhenAvailableAtSameTime() {

        Location pickup = new Location(17.3850, 78.4867);

        Driver close = new Driver("D1", pickup, 4, 0);

        Driver far = new Driver(
                "D2",
                new Location(17.4500, 78.5500),
                4,
                0
        );

        RideRequest ride = new RideRequest(
                "R1", "U1", pickup,
                new Location(17.3900, 78.4900),
                RideType.SOLO, 1, 0, 3600, 0
        );

        DispatchResult result = dispatcher.optimize(
                List.of(far, close),
                List.of(ride)
        );

        assertEquals(
                2,
                result.driverRoutes().get(1).stops().size()
        );
        assertTrue(result.driverRoutes().get(0).stops().isEmpty());
    }

    @Test
    void shouldLeaveRideUnassignedWhenWaitLimitExceeded() {

        Driver driver = new Driver(
                "D1",
                new Location(17.4500, 78.5500),
                4,
                0
        );

        RideRequest ride = new RideRequest(
                "R1", "U1",
                new Location(17.3850, 78.4867),
                new Location(17.3900, 78.4900),
                RideType.SOLO, 1, 0, 60, 0
        );

        DispatchResult result = dispatcher.optimize(
                List.of(driver),
                List.of(ride)
        );

        assertEquals(1, result.unassignedRides().size());
        assertTrue(result.driverRoutes().get(0).stops().isEmpty());
    }

    @Test
    void shouldRejectRideExceedingVehicleCapacity() {

        Driver driver = new Driver(
                "D1",
                new Location(17.3850, 78.4867),
                2,
                0
        );

        RideRequest ride = new RideRequest(
                "R1", "U1",
                new Location(17.3850, 78.4867),
                new Location(17.3900, 78.4900),
                RideType.SOLO, 3, 0, 3600, 0
        );

        DispatchResult result = dispatcher.optimize(
                List.of(driver),
                List.of(ride)
        );

        assertEquals(1, result.unassignedRides().size());
    }
}
