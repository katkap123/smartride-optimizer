
package com.smartride.simulation;

import java.util.List;

import com.smartride.algorithm.GreedyDispatcher;
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

public class DispatchSimulation {

    public static void main(String[] args) {

        DistanceService distanceService = new DistanceService();
        TravelTimeService travelTimeService =
                new TravelTimeService(distanceService);

        GreedyDispatcher greedyDispatcher =
                new GreedyDispatcher(distanceService, travelTimeService);

        WeightedGreedyDispatcher weightedDispatcher =
                new WeightedGreedyDispatcher(
                        distanceService,
                        travelTimeService,
                        1.0,  // Waiting time weight
                        2.0   // Empty distance weight
                );

        Location a = new Location(17.3850, 78.4867);
        Location b = new Location(17.3900, 78.4900);
        Location c = new Location(17.4000, 78.4500);
        Location d = new Location(17.4050, 78.4550);
        Location e = new Location(17.4401, 78.3489);
        Location f = new Location(17.4450, 78.3550);

        List<Driver> drivers = List.of(
                new Driver(
                        "D1",
                        new Location(17.3750, 78.4750),
                        4,
                        0
                ),
                new Driver(
                        "D2",
                        new Location(17.3900, 78.4400),
                        4,
                        0
                ),
                new Driver(
                        "D3",
                        new Location(17.4300, 78.3400),
                        3,
                        0
                )
        );

        List<RideRequest> rides = List.of(
                new RideRequest("R1", "U1", a, b,
                        RideType.SOLO, 1, 0, 1800, 0),
                        
                new RideRequest("R2", "U2", b, c,
                        RideType.SOLO, 1, 0, 1800, 0),

                new RideRequest("R3", "U3", c, d,
                        RideType.SHARED, 1, 0, 1800, 600),

                new RideRequest("R4", "U4", d, a,
                        RideType.SOLO, 1, 0, 1800, 0),

                new RideRequest("R5", "U5", e, f,
                        RideType.SHARED, 1, 0, 1800, 600),

                new RideRequest("R6", "U6", f, e,
                        RideType.SOLO, 1, 0, 1800, 0)
        );

        DispatchResult greedyResult =
                greedyDispatcher.optimize(drivers, rides);

        DispatchResult weightedResult =
                weightedDispatcher.optimize(drivers, rides);

        System.out.println("===== DISPATCH RESULT =====");

        System.out.println("\n===== SUMMARY =====");
       
        printResult("GREEDY DISPATCHER", greedyResult);
        printResult("WEIGHTED GREEDY DISPATCHER", weightedResult);
    }

    
        private static void printResult(
                String algorithmName,
                DispatchResult result) {

                System.out.println("\n===== " + algorithmName + " =====");

                for (DriverRoute route : result.driverRoutes()) {
                        System.out.println("\nDriver: " + route.driverId());

                        for (RouteStop stop : route.stops()) {
                        System.out.printf(
                                "%s %s at %d sec | Occupancy: %d%n",
                                stop.type(),
                                stop.rideId(),
                                stop.arrivalAtSeconds(),
                                stop.occupancyAfterStop()
                        );
                        }

                        System.out.printf(
                                "Total: %.2f km | Empty: %.2f km | Occupied: %.2f km%n",
                                route.totalDistanceKm(),
                                route.emptyDistanceKm(),
                                route.occupiedDistanceKm()
                        );
                }

                double totalEmptyDistance = result.driverRoutes()
                        .stream()
                        .mapToDouble(DriverRoute::emptyDistanceKm)
                        .sum();

                double totalOccupiedDistance = result.driverRoutes()
                        .stream()
                        .mapToDouble(DriverRoute::occupiedDistanceKm)
                        .sum();

                int assignedRides = result.driverRoutes()
                        .stream()
                        .mapToInt(route -> (int) route.stops()
                                .stream()
                                .filter(stop -> stop.type() == StopType.PICKUP)
                                .count())
                        .sum();

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
                        totalEmptyDistance
                );

                System.out.printf(
                        "Occupied distance: %.2f km%n",
                        totalOccupiedDistance
                );

                System.out.printf(
                        "Total rider wait: %.2f minutes%n",
                        result.totalRiderWaitSeconds() / 60.0
                );
        }

}
