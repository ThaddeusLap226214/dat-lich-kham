package com.clinic.gateway.composite;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.stream.Collectors;

@Component
public class PatientClient {

    private final WebClient.Builder webClientBuilder;

    public PatientClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public Flux<RemoteData.Patient> findMine(Long accountId) {
        return webClientBuilder.build().get()
            .uri("http://patient-service/api/v1/patients/mine")
            .header("X-User-Id", String.valueOf(accountId))
            .retrieve()
            .bodyToFlux(RemoteData.Patient.class);
    }

    public Flux<RemoteData.Patient> findPatients(Collection<Long> ids) {
        String joined = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        return webClientBuilder.build().get()
            .uri("http://patient-service/internal/v1/patients?ids={ids}", joined)
            .retrieve()
            .bodyToFlux(RemoteData.Patient.class);
    }
}
