
package com.smartride.controller;

import java.util.List;

import com.smartride.SmartRideApplication;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        classes = SmartRideApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class DispatchControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldRejectNullDriversAndRides() {

        String url = "http://localhost:" + port
                + "/api/dispatch/optimize";

        Map<String, Object> request = Map.of(
                "drivers", java.util.Collections.emptyList(),
                "rides", java.util.Collections.emptyList()
        );

        // Explicit JSON nulls are required to test @NotNull.
        String invalidJson =
                "{\"drivers\":null,\"rides\":null}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity =
                new HttpEntity<>(invalidJson, headers);

        ResponseEntity<Map> response =
                restTemplate.postForEntity(
                        url,
                        entity,
                        Map.class
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                400,
                response.getBody().get("status")
        );

        assertTrue(
                response.getBody().containsKey("message")
        );
    }


    @Test
    void shouldOptimizeValidRideRequest() {

        String url = "http://localhost:" + port
                + "/api/dispatch/optimize";

        String requestJson = """
                {
                "drivers": [
                    {
                    "id": "D1",
                    "currentLocation": {
                        "latitude": 17.3850,
                        "longitude": 78.4867
                    },
                    "capacity": 4,
                    "availableAtSeconds": 0
                    }
                ],
                "rides": [
                    {
                    "id": "R1",
                    "riderId": "Rider1",
                    "pickupLocation": {
                        "latitude": 17.3850,
                        "longitude": 78.4867
                    },
                    "dropoffLocation": {
                        "latitude": 17.3900,
                        "longitude": 78.4900
                    },
                    "rideType": "SOLO",
                    "passengerCount": 1,
                    "requestedAtSeconds": 0,
                    "maxWaitSeconds": 900,
                    "maxDetourSeconds": 0
                    }
                ]
                }
                """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity =
                new HttpEntity<>(requestJson, headers);

        ResponseEntity<Map> response =
                restTemplate.postForEntity(
                        url,
                        entity,
                        Map.class
                );

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        Map<String, Object> responseBody = response.getBody();

        assertNotNull(responseBody);
        Map<String, Object> metrics =
        (Map<String, Object>) responseBody.get("metrics");

        assertNotNull(metrics);

        // One ride should be assigned successfully.
        assertEquals(
                1,
                ((Number) metrics.get("assignedRides")).intValue()
        );

        assertEquals(
                0,
                ((Number) metrics.get("unassignedRides")).intValue()
        );

        // Driver starts at the pickup location.
        // Therefore, there is no empty driving distance.
        assertEquals(
                0.0,
                ((Number) metrics.get("emptyDistancePercentage"))
                        .doubleValue(),
                0.0001
        );

        // Execution time cannot be negative.
        assertTrue(
                ((Number) metrics.get("optimizationDurationMs"))
                        .longValue() >= 0
        );
        }

}
