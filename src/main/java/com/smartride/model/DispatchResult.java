package com.smartride.model;

import java.util.List;

public record DispatchResult(
        List<DriverRoute> driverRoutes,
        List<RideRequest> unassignedRides,
        double totalDistanceKm,
        long totalRiderWaitSeconds
) {
    public DispatchResult {
        if (driverRoutes == null || unassignedRides == null) {
            throw new IllegalArgumentException(
                    "Routes and unassigned rides cannot be null"
            );
        }

        if (!Double.isFinite(totalDistanceKm)
                || totalDistanceKm < 0) {
            throw new IllegalArgumentException(
                    "Total distance cannot be negative"
            );
        }

        if (totalRiderWaitSeconds < 0) {
            throw new IllegalArgumentException(
                    "Total rider wait time cannot be negative"
            );
        }

        driverRoutes = List.copyOf(driverRoutes);
        unassignedRides = List.copyOf(unassignedRides);
    }
}