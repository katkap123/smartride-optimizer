
package com.smartride.simulation;

import java.util.List;

import com.smartride.algorithm.GreedyDispatcher;
import com.smartride.algorithm.TimeAwareDispatcher;
import com.smartride.algorithm.WeightedGreedyDispatcher;
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

public class DynamicAvailabilitySimulation {

    public static void main(String[] args) {

        DistanceService distanceService = new DistanceService();
        TravelTimeService travelTimeService =
                new TravelTimeService(distanceService);

        Location a = new Location(17.3850, 78.4867);
        Location b = new Location(17.3900, 78.4900);
        Location c = new Location(17.4000, 78.4500);
        Location d = new Location(17.4050, 78.4550);
        Location e = new Location(17.4401, 78.3489);
        Location f = new Location(17.4450, 78.3550);

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

        GreedyDispatcher greedy =
                new GreedyDispatcher(
                        distanceService,
                        travelTimeService
                );

        WeightedGreedyDispatcher weighted =
                new WeightedGreedyDispatcher(
                        distanceService,
                        travelTimeService,
                        1.0,
                        3.0
                );
        TimeAwareDispatcher timeAwareDispatcher =
            new TimeAwareDispatcher(
                    distanceService,
                    travelTimeService
            );

        DispatchResult timeAwareResult =
                timeAwareDispatcher.optimize(drivers, rides);

        DispatchResult greedyResult =
                greedy.optimize(drivers, rides);

        DispatchResult weightedResult =
                weighted.optimize(drivers, rides);

        // printSummary("GREEDY", greedyResult);
        // printSummary("WEIGHTED GREEDY", weightedResult);
        System.out.println("\n===== TIME-AWARE DISPATCH =====");

        System.out.println(
                "Assigned rides: " +
                (rides.size() - timeAwareResult.unassignedRides().size())
        );

        System.out.println(
                "Unassigned rides: " +
                timeAwareResult.unassignedRides().size()
        );

        System.out.printf(
                "Total distance: %.2f km%n",
                timeAwareResult.totalDistanceKm()
        );

        double emptyDistance = timeAwareResult.driverRoutes().stream()
                .mapToDouble(DriverRoute::emptyDistanceKm)
                .sum();

        System.out.printf(
                "Empty distance: %.2f km%n",
                emptyDistance
        );

        System.out.printf(
                "Total rider wait: %.2f minutes%n",
                timeAwareResult.totalRiderWaitSeconds() / 60.0
        );


        System.out.println("\n===== TIME-AWARE DRIVER ROUTES =====");

        for (DriverRoute route : timeAwareResult.driverRoutes()) {

            System.out.printf(
                    "%nDriver: %s | Empty: %.2f km | Total: %.2f km%n",
                    route.driverId(),
                    route.emptyDistanceKm(),
                    route.totalDistanceKm()
            );

            for (RouteStop stop : route.stops()) {
                System.out.printf(
                        "  Ride: %s | %s | Time: %d sec%n",
                        stop.rideId(),
                        stop.type(),
                        stop.arrivalAtSeconds()
                );
            }
        }


        double[][] weightConfigurations = {
                {1.0, 0.0},
                {1.0, 2.0},
                {1.0, 3.0}
        };

        System.out.println(
                "\n===== TIME-AWARE WEIGHT COMPARISON ====="
        );

        System.out.printf(
                "%-12s %-12s %-14s %-14s %-14s%n",
                "Weights",
                "Assigned",
                "Total KM",
                "Empty KM",
                "Wait (min)"
        );

        for (double[] weights : weightConfigurations) {

            TimeAwareDispatcher dispatcher =
                    new TimeAwareDispatcher(
                            distanceService,
                            travelTimeService,
                            weights[0],
                            weights[1]
                    );

            DispatchResult result = dispatcher.optimize(drivers, rides);

            int assigned = rides.size()
                    - result.unassignedRides().size();

            double emptyDistanceKm = result.driverRoutes().stream()
                    .mapToDouble(DriverRoute::emptyDistanceKm)
                    .sum();

            System.out.printf(
                    "%-12s %-12d %-14.2f %-14.2f %-14.2f%n",
                    "(" + weights[0] + "," + weights[1] + ")",
                    assigned,
                    result.totalDistanceKm(),
                    emptyDistanceKm,
                    result.totalRiderWaitSeconds() / 60.0
            );
        }

    }

    private static void printSummary(
            String name,
            DispatchResult result) {

        System.out.println("\n===== " + name + " =====");

        for (DriverRoute route : result.driverRoutes()) {

            System.out.println("Driver: " + route.driverId());

            for (RouteStop stop : route.stops()) {
                System.out.printf(
                        "  %s %s at %d sec%n",
                        stop.type(),
                        stop.rideId(),
                        stop.arrivalAtSeconds()
                );
            }
        }

        double emptyDistance = result.driverRoutes()
                .stream()
                .mapToDouble(DriverRoute::emptyDistanceKm)
                .sum();

        long assignedRides = result.driverRoutes()
                .stream()
                .flatMap(route -> route.stops().stream())
                .filter(stop -> stop.type() == StopType.PICKUP)
                .count();

        System.out.println("\n===== SUMMARY =====");

        System.out.println("Assigned rides: " + assignedRides);
        System.out.println(
                "Unassigned rides: " + result.unassignedRides().size()
        );

        System.out.printf(
                "Total distance: %.2f km%n",
                result.totalDistanceKm()
        );

        System.out.printf(
                "Empty distance: %.2f km%n",
                emptyDistance
        );

        System.out.printf(
                "Total rider wait: %.2f minutes%n",
                result.totalRiderWaitSeconds() / 60.0
        );
    }
}
