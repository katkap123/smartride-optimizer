package com.smartride.model;

public record Location(
        double latitude,
        double longitude
) {
    public Location {
        if (!Double.isFinite(latitude)
                || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException(
                    "Invalid latitude"
            );
        }

        if (!Double.isFinite(longitude)
                || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException(
                    "Invalid longitude"
            );
        }
    }
}