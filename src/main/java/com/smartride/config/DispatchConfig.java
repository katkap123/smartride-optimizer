
package com.smartride.config;

import com.smartride.algorithm.TimeAwareDispatcher;
import com.smartride.service.DistanceService;
import com.smartride.service.TravelTimeService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
@Configuration
public class DispatchConfig {

    @Bean
    public DistanceService distanceService() {
        return new DistanceService();
    }

    @Bean
    public TravelTimeService travelTimeService(
            DistanceService distanceService) {

        return new TravelTimeService(distanceService);
    }

    
    @Bean
    public TimeAwareDispatcher timeAwareDispatcher(
            DistanceService distanceService,
            TravelTimeService travelTimeService,
            @Value("${dispatch.weight.wait:1.0}")
            double waitWeight,
            @Value("${dispatch.weight.empty-distance:0.0}")
            double emptyDistanceWeight) {

        return new TimeAwareDispatcher(
                distanceService,
                travelTimeService,
                waitWeight,
                emptyDistanceWeight
        );
    }

}
