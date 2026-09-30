package com.clinic.reminder.client;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PatientClient {

    private final RestClient restClient;

    public PatientClient(@LoadBalanced RestClient.Builder loadBalancedRestClientBuilder) {
        this.restClient = loadBalancedRestClientBuilder.baseUrl("http://patient-service").build();
    }

    public List<PatientContact> findByIds(Collection<Long> ids) {
        String joined = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        return restClient.get()
            .uri("/internal/v1/patients?ids={ids}", joined)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {
            });
    }
}
