package com.smartride.service;

import com.smartride.model.Location;

public class TravelTimeService {

    private static final double DEFAULT_SPEED_KMPH = 30.0;

    private final DistanceService distanceService;

    public TravelTimeService(DistanceService distanceService) {
        if (distanceService == null) {
            throw new IllegalArgumentException(
                    "DistanceService cannot be null"
            );
        }

        this.distanceService = distanceService;
    }

    public long estimateTravelTimeSeconds(
            Location from,
            Location to) {

        double distanceKm =
                distanceService.calculateDistance(from, to);

        double travelTimeSeconds =
                (distanceKm / DEFAULT_SPEED_KMPH) * 3600;

        return (long) Math.ceil(travelTimeSeconds);
    }
}