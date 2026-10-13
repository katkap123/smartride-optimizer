package com.smartride.model;

import java.util.Objects;

public record DispatchEvent(
        long eventTimeSeconds,
        DispatchEventType eventType,
        String entityId
) {
    public DispatchEvent {
        if (eventTimeSeconds < 0) {
            throw new IllegalArgumentException(
                    "Event time cannot be negative"
            );
        }

        Objects.requireNonNull(eventType, "Event type cannot be null");

        if (entityId == null || entityId.isBlank()) {
            throw new IllegalArgumentException(
                    "Entity ID cannot be null or blank"
            );
        }
    }
}