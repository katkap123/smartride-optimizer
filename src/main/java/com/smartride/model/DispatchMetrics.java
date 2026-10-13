
package com.smartride.model;

public record DispatchMetrics(
        long optimizationDurationMs,
        double emptyDistancePercentage,
        int assignedRides,
        int unassignedRides
) {}
