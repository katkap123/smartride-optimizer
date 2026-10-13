
package com.smartride.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.Valid;

public record Driver(
        @NotBlank String id,
        @Valid @NotNull Location currentLocation,
        @Positive int capacity,
        @PositiveOrZero long availableAtSeconds
) {}
