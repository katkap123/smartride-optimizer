package com.smartride.model;

public record Driver(
        String id,
        Location currentLocation,
        int capacity,
        long availableAtSeconds
) {
    public Driver {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                    "Driver ID is required"
            );
        }

        if (currentLocation == null) {
            throw new IllegalArgumentException(
                    "Driver location is required"
            );
        }

        if (capacity <= 0) {
            throw new IllegalArgumentException(
                    "Driver capacity must be greater than zero"
            );
        }

        if (availableAtSeconds < 0) {
            throw new IllegalArgumentException(
                    "Available time cannot be negative"
            );
        }
    }
}