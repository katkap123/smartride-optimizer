
package com.smartride.model;

public record DispatchOptimizationResponse(
        DispatchResult result,
        DispatchMetrics metrics
) {}
