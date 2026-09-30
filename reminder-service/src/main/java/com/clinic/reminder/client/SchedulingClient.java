package com.clinic.reminder.client;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class SchedulingClient {

    private final RestClient restClient;

    public SchedulingClient(@LoadBalanced RestClient.Builder loadBalancedRestClientBuilder) {
        this.restClient = loadBalancedRestClientBuilder.baseUrl("http://scheduling-service").build();
    }

    public List<AppointmentInfo> findConfirmedBetween(LocalDateTime from, LocalDateTime to) {
        return restClient.get()
            .uri("/internal/v1/appointments?status=CONFIRMED&from={from}&to={to}", from, to)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {
            });
    }
}
