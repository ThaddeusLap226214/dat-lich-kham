package com.clinic.scheduling.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

@Component
public class ProviderClient {

    private static final Logger log = LoggerFactory.getLogger(ProviderClient.class);
    private final RestClient restClient;

    public ProviderClient(@LoadBalanced RestClient.Builder loadBalancedRestClientBuilder) {
        this.restClient = loadBalancedRestClientBuilder.baseUrl("http://provider-service").build();
    }

    @Cacheable(value = "workingHours", key = "{#doctorId, #date}")
    public WorkingHours getWorkingHours(Long doctorId, LocalDate date) {
        log.info("Calling provider-service for doctorId={}, date={}", doctorId, date);
        return restClient.get()
            .uri("/internal/v1/doctors/{id}/working-hours?date={date}", doctorId, date)
            .retrieve()
            .body(WorkingHours.class);
    }
}
