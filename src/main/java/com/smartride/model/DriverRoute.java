
package com.smartride.model;

import java.util.List;

public record DriverRoute(
        String driverId,
        List<RouteStop> stops,
        double totalDistanceKm,
        double emptyDistanceKm,
        double occupiedDistanceKm,
        long completedAtSeconds
) {

    public DriverRoute {

        if (driverId == null || driverId.isBlank()) {
            throw new IllegalArgumentException(
                    "Driver ID cannot be empty"
            );
        }

        if (stops == null) {
            throw new IllegalArgumentException(
                    "Stops cannot be null"
            );
        }

        if (!Double.isFinite(totalDistanceKm)
                || !Double.isFinite(emptyDistanceKm)
                || !Double.isFinite(occupiedDistanceKm)
                || totalDistanceKm < 0
                || emptyDistanceKm < 0
                || occupiedDistanceKm < 0) {
            throw new IllegalArgumentException(
                    "Distances must be finite and non-negative"
            );
        }

        if (Math.abs(
                totalDistanceKm - (emptyDistanceKm + occupiedDistanceKm)
        ) > 0.000001) {
            throw new IllegalArgumentException(
                    "Total distance must equal empty + occupied distance"
            );
        }

        if (completedAtSeconds < 0) {
            throw new IllegalArgumentException(
                    "Completion time cannot be negative"
            );
        }

        stops = List.copyOf(stops);
    }
}
