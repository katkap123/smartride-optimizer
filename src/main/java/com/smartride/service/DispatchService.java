
package com.smartride.service;

import com.smartride.algorithm.TimeAwareDispatcher;
import com.smartride.model.DispatchResult;
import com.smartride.model.Driver;
import com.smartride.model.RideRequest;

import org.springframework.stereotype.Service;

import java.util.List;

import com.smartride.model.DispatchMetrics;
import com.smartride.model.DispatchOptimizationResponse;
import com.smartride.model.DriverRoute;

@Service
public class DispatchService {

    private final TimeAwareDispatcher dispatcher;

    public DispatchService(TimeAwareDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    
    public DispatchOptimizationResponse optimize(
            List<Driver> drivers,
            List<RideRequest> rides) {

        long startTime = System.nanoTime();

        DispatchResult result =
                dispatcher.optimize(drivers, rides);

        long durationNanos = System.nanoTime() - startTime;
        long durationMs = durationNanos / 1_000_000;

        double emptyDistanceKm = result.driverRoutes()
                .stream()
                .mapToDouble(DriverRoute::emptyDistanceKm)
                .sum();

        double emptyDistancePercentage =
                result.totalDistanceKm() > 0
                        ? (emptyDistanceKm / result.totalDistanceKm()) * 100
                        : 0.0;

        int unassigned = result.unassignedRides().size();
        int assigned = rides.size() - unassigned;

        DispatchMetrics metrics = new DispatchMetrics(
                durationMs,
                emptyDistancePercentage,
                assigned,
                unassigned
        );

        return new DispatchOptimizationResponse(result, metrics);
    }

}
