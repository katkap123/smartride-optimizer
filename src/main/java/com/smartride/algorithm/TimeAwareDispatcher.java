
package com.smartride.algorithm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.smartride.model.DispatchEvent;
import com.smartride.model.DispatchEventType;
import com.smartride.model.DispatchResult;
import com.smartride.model.Driver;
import com.smartride.model.DriverRoute;
import com.smartride.model.Location;
import com.smartride.model.RideRequest;
import com.smartride.model.RouteStop;
import com.smartride.model.StopType;
import com.smartride.service.DistanceService;
import com.smartride.service.TravelTimeService;
import com.smartride.simulation.DispatchEventQueue;

public class TimeAwareDispatcher {

    private final DistanceService distanceService;
    private final TravelTimeService travelTimeService;

    private final double waitWeight;
    private final double emptyDistanceWeight;

    public TimeAwareDispatcher(
            DistanceService distanceService,
            TravelTimeService travelTimeService) {

        this(distanceService, travelTimeService, 1.0, 0.0);
    }

    public TimeAwareDispatcher(
            DistanceService distanceService,
            TravelTimeService travelTimeService,
            double waitWeight,
            double emptyDistanceWeight) {

        this.distanceService = Objects.requireNonNull(distanceService);
        this.travelTimeService = Objects.requireNonNull(travelTimeService);

        if (!Double.isFinite(waitWeight)
                || !Double.isFinite(emptyDistanceWeight)
                || waitWeight < 0
                || emptyDistanceWeight < 0
                || (waitWeight == 0 && emptyDistanceWeight == 0)) {

            throw new IllegalArgumentException(
                    "Weights must be finite, non-negative, and not both zero"
            );
        }

        this.waitWeight = waitWeight;
        this.emptyDistanceWeight = emptyDistanceWeight;
    }

    private double calculateScore(
        long waitSeconds,
        double emptyDistanceKm) {

        return waitWeight * (waitSeconds / 60.0)
                + emptyDistanceWeight * emptyDistanceKm;
    }

    public DispatchResult optimize(
            List<Driver> drivers,
            List<RideRequest> rideRequests) {

        Objects.requireNonNull(drivers);
        Objects.requireNonNull(rideRequests);

        DispatchEventQueue eventQueue = new DispatchEventQueue();

        Map<String, DriverState> driverStates = new LinkedHashMap<>();
        Map<String, RideRequest> rideMap = new HashMap<>();
        Map<String, RideRequest> pendingRides = new LinkedHashMap<>();
        Set<String> expiredRideIds = new HashSet<>();

        for (Driver driver : drivers) {

            if (driverStates.putIfAbsent(
                    driver.id(),
                    new DriverState(driver)) != null) {

                throw new IllegalArgumentException(
                        "Duplicate driver ID: " + driver.id()
                );
            }

            eventQueue.add(new DispatchEvent(
                    driver.availableAtSeconds(),
                    DispatchEventType.DRIVER_AVAILABLE,
                    driver.id()
            ));
        }

        for (RideRequest ride : rideRequests) {
            if (rideMap.putIfAbsent(ride.id(), ride) != null) {
                throw new IllegalArgumentException(
                        "Duplicate ride ID: " + ride.id()
                );
            }

            eventQueue.add(new DispatchEvent(
                    ride.requestedAtSeconds(),
                    DispatchEventType.RIDE_REQUESTED,
                    ride.id()
            ));

            eventQueue.add(new DispatchEvent(
                    ride.requestedAtSeconds() + ride.maxWaitSeconds(),
                    DispatchEventType.RIDE_EXPIRED,
                    ride.id()
            ));
        }

        
    long totalRiderWaitSeconds = 0;

    while (!eventQueue.isEmpty()) {

        long currentTime = eventQueue.peek().eventTimeSeconds();

        List<DispatchEvent> currentEvents = new ArrayList<>();

        while (!eventQueue.isEmpty()
                    && eventQueue.peek().eventTimeSeconds() == currentTime) {

                currentEvents.add(eventQueue.poll());
            }

        // Add all ride requests arriving at this timestamp.
        for (DispatchEvent event : currentEvents) {

            if (event.eventType() == DispatchEventType.RIDE_REQUESTED) {

                RideRequest ride = rideMap.get(event.entityId());
                pendingRides.put(ride.id(), ride);
            }
        }

        // Attempt assignments for all pending rides.
        List<RideRequest> ridesToAssign =
                new ArrayList<>(pendingRides.values());

        
    // Repeatedly select the best feasible driver-ride pair
    // among requests that have already arrived.
    while (!pendingRides.isEmpty()) {

        AssignmentCandidate bestAssignment =
                findBestAssignment(
                        pendingRides,
                        driverStates,
                        currentTime
                );

        if (bestAssignment == null) {
            // No feasible driver-ride pair at this timestamp.
            // Keep remaining rides pending for future events.
            break;
        }

        RideRequest ride = bestAssignment.ride();

        DriverCandidate candidate =
                bestAssignment.driverCandidate();

        assignRide(ride, candidate);

        totalRiderWaitSeconds += candidate.waitSeconds();

        pendingRides.remove(ride.id());

        // Driver becomes eligible for another assignment
        // only after the current ride is completed.
        eventQueue.add(new DispatchEvent(
                candidate.dropoffAtSeconds(),
                DispatchEventType.DRIVER_AVAILABLE,
                candidate.driverState().driver.id()
        ));
    }

        // Expire rides only after considering assignments at this timestamp.
        for (DispatchEvent event : currentEvents) {

            if (event.eventType() == DispatchEventType.RIDE_EXPIRED) {

                String rideId = event.entityId();

                if (pendingRides.remove(rideId) != null) {
                    expiredRideIds.add(rideId);
                }
            }
        }   
    }

    List<DriverRoute> routes = driverStates.values().stream()
            .map(DriverState::toDriverRoute)
            .toList();

    List<RideRequest> unassignedRides = rideRequests.stream()
            .filter(ride -> expiredRideIds.contains(ride.id()))
            .toList();

    return new DispatchResult(
            routes,
            unassignedRides,
            routes.stream()
                    .mapToDouble(DriverRoute::totalDistanceKm)
                    .sum(),
            totalRiderWaitSeconds
    );
    }

    private AssignmentCandidate findBestAssignment(
            Map<String, RideRequest> pendingRides,
            Map<String, DriverState> driverStates,
            long currentTime) {

        AssignmentCandidate best = null;

        for (RideRequest ride : pendingRides.values()) {

            for (DriverState driver : driverStates.values()) {

                if (ride.passengerCount() > driver.driver.capacity()) {
                    continue;
                }

                // Strict event-driven: only currently available drivers.
                if (driver.availableAt > currentTime) {
                    continue;
                }

                long pickupTravelSeconds =
                        travelTimeService.estimateTravelTimeSeconds(
                                driver.location,
                                ride.pickupLocation()
                        );

                long pickupAt = currentTime + pickupTravelSeconds;

                long waitSeconds =
                        pickupAt - ride.requestedAtSeconds();

                if (waitSeconds > ride.maxWaitSeconds()) {
                    continue;
                }

                long tripDurationSeconds =
                        travelTimeService.estimateTravelTimeSeconds(
                                ride.pickupLocation(),
                                ride.dropoffLocation()
                        );

                long dropoffAt = pickupAt + tripDurationSeconds;

                double emptyDistanceKm =
                        distanceService.calculateDistance(
                                driver.location,
                                ride.pickupLocation()
                        );

                double occupiedDistanceKm =
                        distanceService.calculateDistance(
                                ride.pickupLocation(),
                                ride.dropoffLocation()
                        );

                DriverCandidate candidate = new DriverCandidate(
                        driver,
                        pickupAt,
                        dropoffAt,
                        waitSeconds,
                        emptyDistanceKm,
                        occupiedDistanceKm
                );

                double score = calculateScore(
                        waitSeconds,
                        emptyDistanceKm
                );

                AssignmentCandidate assignment =
                        new AssignmentCandidate(ride, candidate, score);

                if (best == null
                        || Comparator
                                .comparingDouble(AssignmentCandidate::score)
                                .thenComparing(a ->
                                        a.driverCandidate().pickupAtSeconds())
                                .thenComparing(a -> a.ride().id())
                                .thenComparing(a ->
                                        a.driverCandidate()
                                                .driverState().driver.id())
                                .compare(assignment, best) < 0) {

                    best = assignment;
                }
            }
        }

        return best;
    }



    private DriverCandidate findBestDriver(
            RideRequest ride,
            Map<String, DriverState> driverStates,
            long currentTime) {

        DriverCandidate bestCandidate = null;

        for (DriverState driver : driverStates.values()) {

            if (ride.passengerCount() > driver.driver.capacity()) {
                continue;
            }

            if (driver.availableAt > currentTime) {
                continue;
            }

            long departureAt = currentTime;

            long travelToPickupSeconds =
                    travelTimeService.estimateTravelTimeSeconds(
                            driver.location,
                            ride.pickupLocation()
                    );

            long pickupAt = departureAt + travelToPickupSeconds;

            long waitSeconds = pickupAt - ride.requestedAtSeconds();

            if (waitSeconds > ride.maxWaitSeconds()) {
                continue;
            }

            long tripDurationSeconds =
                    travelTimeService.estimateTravelTimeSeconds(
                            ride.pickupLocation(),
                            ride.dropoffLocation()
                    );

            long dropoffAt = pickupAt + tripDurationSeconds;

            double emptyDistanceKm =
                    distanceService.calculateDistance(
                            driver.location,
                            ride.pickupLocation()
                    );

            double occupiedDistanceKm =
                    distanceService.calculateDistance(
                            ride.pickupLocation(),
                            ride.dropoffLocation()
                    );

            DriverCandidate candidate = new DriverCandidate(
                    driver,
                    pickupAt,
                    dropoffAt,
                    waitSeconds,
                    emptyDistanceKm,
                    occupiedDistanceKm
            );

            
        double candidateScore = calculateScore(
                candidate.waitSeconds(),
                candidate.emptyDistanceKm()
        );

        if (bestCandidate == null) {
            bestCandidate = candidate;
            continue;
        }

        double bestScore = calculateScore(
                bestCandidate.waitSeconds(),
                bestCandidate.emptyDistanceKm()
        );

        int scoreComparison = Double.compare(
                candidateScore,
                bestScore
        );

        if (scoreComparison < 0
                || (scoreComparison == 0
                    && candidate.pickupAtSeconds()
                            < bestCandidate.pickupAtSeconds())
                || (scoreComparison == 0
                    && candidate.pickupAtSeconds()
                            == bestCandidate.pickupAtSeconds()
                    && candidate.driverState().driver.id().compareTo(
                            bestCandidate.driverState().driver.id()) < 0)) {

            bestCandidate = candidate;
        }

        }

        return bestCandidate;
    }

    private void assignRide(
            RideRequest ride,
            DriverCandidate candidate) {

        DriverState driver = candidate.driverState();

        driver.stops.add(new RouteStop(
                ride.id(),
                StopType.PICKUP,
                ride.pickupLocation(),
                candidate.pickupAtSeconds(),
                ride.passengerCount()
        ));

        driver.stops.add(new RouteStop(
                ride.id(),
                StopType.DROPOFF,
                ride.dropoffLocation(),
                candidate.dropoffAtSeconds(),
                0
        ));

        driver.emptyDistanceKm += candidate.emptyDistanceKm();
        driver.occupiedDistanceKm += candidate.occupiedDistanceKm();

        driver.location = ride.dropoffLocation();
        driver.availableAt = candidate.dropoffAtSeconds();
    }


    private record DriverCandidate(
            DriverState driverState,
            long pickupAtSeconds,
            long dropoffAtSeconds,
            long waitSeconds,
            double emptyDistanceKm,
            double occupiedDistanceKm
    ) {}

    private record AssignmentCandidate(
            RideRequest ride,
            DriverCandidate driverCandidate,
            double score
    ) {}


    private static class DriverState {

        final Driver driver;
        final List<RouteStop> stops = new ArrayList<>();

        Location location;
        long availableAt;

        double emptyDistanceKm;
        double occupiedDistanceKm;

        DriverState(Driver driver) {
            this.driver = driver;
            this.location = driver.currentLocation();
            this.availableAt = driver.availableAtSeconds();
        }

        double totalDistanceKm() {
            return emptyDistanceKm + occupiedDistanceKm;
        }

        DriverRoute toDriverRoute() {
            return new DriverRoute(
                    driver.id(),
                    stops,
                    totalDistanceKm(),
                    emptyDistanceKm,
                    occupiedDistanceKm,
                    availableAt
            );
        }
    }

}
