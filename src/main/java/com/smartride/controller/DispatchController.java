
package com.smartride.controller;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.smartride.algorithm.TimeAwareDispatcher;
import com.smartride.model.DispatchOptimizationResponse;
import com.smartride.model.DispatchResult;
import com.smartride.model.Driver;
import com.smartride.model.RideRequest;
import com.smartride.service.DispatchService;
import com.smartride.service.DistanceService;
import com.smartride.service.TravelTimeService;

@RestController
@RequestMapping("/api/dispatch")
public class DispatchController {
    
    private final DispatchService dispatchService;

    public DispatchController(DispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    
    @PostMapping("/optimize")
    public DispatchOptimizationResponse optimize(
            @Valid @RequestBody DispatchRequest request) {

        return dispatchService.optimize(
                request.drivers(),
                request.rides()
        );
    }

    public record DispatchRequest(
            @NotNull List<@Valid Driver> drivers,
            @NotNull List<@Valid RideRequest> rides
    ) {}

}
