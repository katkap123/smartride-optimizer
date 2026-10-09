package com.smartride.model;

public record RideRequest(
        String id,
        String riderId,
        Location pickupLocation,
        Location dropoffLocation,
        RideType rideType,
        int passengerCount,
        long requestedAtSeconds,
        long maxWaitSeconds,
        long maxDetourSeconds
) {
    public RideRequest {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                    "Ride ID is required"
            );
        }

        if (riderId == null || riderId.isBlank()) {
            throw new IllegalArgumentException(
                    "Rider ID is required"
            );
        }

        if (pickupLocation == null || dropoffLocation == null) {
            throw new IllegalArgumentException(
                    "Pickup and dropoff locations are required"
            );
        }

        if (rideType == null) {
            throw new IllegalArgumentException(
                    "Ride type is required"
            );
        }

        if (passengerCount <= 0) {
            throw new IllegalArgumentException(
                    "Passenger count must be positive"
            );
        }

        if (requestedAtSeconds < 0
                || maxWaitSeconds < 0
                || maxDetourSeconds < 0) {
            throw new IllegalArgumentException(
                    "Ride timing constraints cannot be negative"
            );
        }
    }
}