
package com.smartride.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record RideRequest(
        @NotBlank String id,
        @NotBlank String riderId,
        @Valid @NotNull Location pickupLocation,
        @Valid @NotNull Location dropoffLocation,
        @NotNull RideType rideType,
        @Positive int passengerCount,
        @PositiveOrZero long requestedAtSeconds,
        @PositiveOrZero long maxWaitSeconds,
        @PositiveOrZero long maxDetourSeconds
) {}
