package com.clinic.scheduling.client;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

@Component
public class ProviderClient {

    private final RestClient restClient;

    public ProviderClient(@LoadBalanced RestClient.Builder loadBalancedRestClientBuilder) {
        this.restClient = loadBalancedRestClientBuilder.baseUrl("http://provider-service").build();
    }

    public WorkingHours getWorkingHours(Long doctorId, LocalDate date) {
        return restClient.get()
            .uri("/internal/v1/doctors/{id}/working-hours?date={date}", doctorId, date)
            .retrieve()
            .body(WorkingHours.class);
    }
}
