package com.smartride.model;

public record RouteStop(
        String rideId,
        StopType type,
        Location location,
        long arrivalAtSeconds,
        int occupancyAfterStop
) {
    public RouteStop {
        if (rideId == null || rideId.isBlank()) {
            throw new IllegalArgumentException(
                    "Ride ID is required"
            );
        }

        if (type == null) {
            throw new IllegalArgumentException(
                    "Stop type is required"
            );
        }

        if (location == null) {
            throw new IllegalArgumentException(
                    "Stop location is required"
            );
        }

        if (arrivalAtSeconds < 0) {
            throw new IllegalArgumentException(
                    "Arrival time cannot be negative"
            );
        }

        if (occupancyAfterStop < 0) {
            throw new IllegalArgumentException(
                    "Passenger occupancy cannot be negative"
            );
        }
    }
}