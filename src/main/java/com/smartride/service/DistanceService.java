package com.smartride.service;

import com.smartride.model.Location;

public class DistanceService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    public double calculateDistance(Location from, Location to) {

        if (from == null || to == null) {
            throw new IllegalArgumentException(
                    "Locations cannot be null"
            );
        }

        double lat1 = Math.toRadians(from.latitude());
        double lat2 = Math.toRadians(to.latitude());

        double deltaLat = lat2 - lat1;
        double deltaLon = Math.toRadians(
                to.longitude() - from.longitude()
        );

        double a = Math.pow(Math.sin(deltaLat / 2), 2)
                + Math.cos(lat1)
                * Math.cos(lat2)
                * Math.pow(Math.sin(deltaLon / 2), 2);

        a = Math.max(0.0, Math.min(1.0, a));

        double c = 2 * Math.atan2(
                Math.sqrt(a),
                Math.sqrt(1 - a)
        );

        return EARTH_RADIUS_KM * c;
    }
}