
package com.smartride.algorithm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.smartride.model.DispatchResult;
import com.smartride.model.Driver;
import com.smartride.model.DriverRoute;
import com.smartride.model.Location;
import com.smartride.model.RideRequest;
import com.smartride.model.RouteStop;
import com.smartride.model.StopType;
import com.smartride.service.DistanceService;
import com.smartride.service.TravelTimeService;

public class WeightedGreedyDispatcher {

    private final DistanceService distanceService;
    private final TravelTimeService travelTimeService;

    private final double waitWeight;
    private final double emptyDistanceWeight;

    public WeightedGreedyDispatcher(
            DistanceService distanceService,
            TravelTimeService travelTimeService,
            double waitWeight,
            double emptyDistanceWeight) {

        this.distanceService =
                Objects.requireNonNull(distanceService);

        this.travelTimeService =
                Objects.requireNonNull(travelTimeService);

        if (!Double.isFinite(waitWeight)
                || !Double.isFinite(emptyDistanceWeight)
                || waitWeight < 0
                || emptyDistanceWeight < 0
                || (waitWeight == 0 && emptyDistanceWeight == 0)) {

            throw new IllegalArgumentException(
                    "Weights must be finite, non-negative, "
                    + "and at least one must be positive"
            );
        }

        this.waitWeight = waitWeight;
        this.emptyDistanceWeight = emptyDistanceWeight;
    }

    public double calculateScore(
            long riderWaitSeconds,
            double emptyDistanceKm) {

        if (riderWaitSeconds < 0
                || !Double.isFinite(emptyDistanceKm)
                || emptyDistanceKm < 0) {

            throw new IllegalArgumentException(
                    "Wait time and distance must be non-negative"
            );
        }

        double waitMinutes = riderWaitSeconds / 60.0;

        return waitWeight * waitMinutes
                + emptyDistanceWeight * emptyDistanceKm;
    }

    public DispatchResult optimize(
            List<Driver> drivers,
            List<RideRequest> rideRequests) {

        Objects.requireNonNull(drivers);
        Objects.requireNonNull(rideRequests);

        Map<String, DriverState> states = new LinkedHashMap<>();

        for (Driver driver : drivers) {
            if (states.containsKey(driver.id())) {
                throw new IllegalArgumentException("Duplicate driver ID");
            }
            states.put(driver.id(), new DriverState(driver));
        }

        List<RideRequest> remaining = new ArrayList<>(rideRequests);
        Set<String> rideIds = new HashSet<>();

        for (RideRequest ride : remaining) {
            if (!rideIds.add(ride.id())) {
                throw new IllegalArgumentException("Duplicate ride ID");
            }
        }

        long totalWaitSeconds = 0;

        while (!remaining.isEmpty()) {

            Candidate best = null;

            for (DriverState state : states.values()) {
                for (RideRequest ride : remaining) {

                    if (ride.passengerCount() > state.driver.capacity()) {
                        continue;
                    }

                    double pickupDistance =
                            distanceService.calculateDistance(
                                    state.location,
                                    ride.pickupLocation()
                            );

                    long travelToPickup =
                            travelTimeService.estimateTravelTimeSeconds(
                                    state.location,
                                    ride.pickupLocation()
                            );

                    long departureTime = Math.max(
                            state.availableAt,
                            ride.requestedAtSeconds()
                    );

                    long pickupTime = departureTime + travelToPickup;
                    long waitTime = pickupTime - ride.requestedAtSeconds();

                    if (waitTime > ride.maxWaitSeconds()) {
                        continue;
                    }

                    double score = calculateScore(
                            waitTime,
                            pickupDistance
                    );

                    Candidate candidate = new Candidate(
                            state,
                            ride,
                            pickupDistance,
                            pickupTime,
                            waitTime,
                            score
                    );

                    if (best == null || isBetter(candidate, best)) {
                        best = candidate;
                    }
                }
            }

            if (best == null) {
                break;
            }

            DriverState state = best.state;
            RideRequest ride = best.ride;

            long tripDuration =
                    travelTimeService.estimateTravelTimeSeconds(
                            ride.pickupLocation(),
                            ride.dropoffLocation()
                    );

            long dropoffTime = best.pickupTime + tripDuration;

            state.stops.add(new RouteStop(
                    ride.id(),
                    StopType.PICKUP,
                    ride.pickupLocation(),
                    best.pickupTime,
                    ride.passengerCount()
            ));

            state.stops.add(new RouteStop(
                    ride.id(),
                    StopType.DROPOFF,
                    ride.dropoffLocation(),
                    dropoffTime,
                    0
            ));

            double tripDistance =
                    distanceService.calculateDistance(
                            ride.pickupLocation(),
                            ride.dropoffLocation()
                    );

            state.emptyDistanceKm += best.pickupDistance;
            state.occupiedDistanceKm += tripDistance;

            state.totalDistanceKm =
                    state.emptyDistanceKm + state.occupiedDistanceKm;

            state.location = ride.dropoffLocation();
            state.availableAt = dropoffTime;

            totalWaitSeconds += best.waitTime;

            remaining.remove(ride);
        }

        List<DriverRoute> routes = new ArrayList<>();
        double totalDistanceKm = 0;

        for (DriverState state : states.values()) {

            routes.add(new DriverRoute(
                    state.driver.id(),
                    state.stops,
                    state.totalDistanceKm,
                    state.emptyDistanceKm,
                    state.occupiedDistanceKm,
                    state.availableAt
            ));

            totalDistanceKm += state.totalDistanceKm;
        }

        return new DispatchResult(
                routes,
                remaining,
                totalDistanceKm,
                totalWaitSeconds
        );
    }


    private boolean isBetter(Candidate a, Candidate b) {

        int scoreComparison = Double.compare(a.score, b.score);

        if (scoreComparison != 0) {
            return scoreComparison < 0;
        }

        // Tie-breaker 1: lower rider waiting time.
        if (a.waitTime != b.waitTime) {
            return a.waitTime < b.waitTime;
        }

        // Tie-breaker 2: shorter empty driving distance.
        int distanceComparison = Double.compare(
                a.pickupDistance,
                b.pickupDistance
        );

        if (distanceComparison != 0) {
            return distanceComparison < 0;
        }

        // Deterministic tie-breaking.
        int driverComparison = a.state.driver.id()
                .compareTo(b.state.driver.id());

        if (driverComparison != 0) {
            return driverComparison < 0;
        }

        return a.ride.id().compareTo(b.ride.id()) < 0;
    }


    private static class DriverState {

        final Driver driver;
        final List<RouteStop> stops = new ArrayList<>();

        Location location;
        long availableAt;

        double totalDistanceKm;
        double emptyDistanceKm;
        double occupiedDistanceKm;

        DriverState(Driver driver) {
            this.driver = driver;
            this.location = driver.currentLocation();
            this.availableAt = driver.availableAtSeconds();
        }
    }

    private record Candidate(
            DriverState state,
            RideRequest ride,
            double pickupDistance,
            long pickupTime,
            long waitTime,
            double score
    ) {}


}
