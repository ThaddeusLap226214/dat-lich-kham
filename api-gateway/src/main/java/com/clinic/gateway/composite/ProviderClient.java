package com.clinic.gateway.composite;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.stream.Collectors;

@Component
public class ProviderClient {

    private final WebClient.Builder webClientBuilder;

    public ProviderClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public Flux<RemoteData.Doctor> findDoctors(Collection<Long> ids) {
        return webClientBuilder.build().get()
            .uri("http://provider-service/internal/v1/doctors?ids={ids}", join(ids))
            .retrieve()
            .bodyToFlux(RemoteData.Doctor.class);
    }

    public Flux<RemoteData.Room> findRooms(Collection<Long> ids) {
        return webClientBuilder.build().get()
            .uri("http://provider-service/internal/v1/rooms?ids={ids}", join(ids))
            .retrieve()
            .bodyToFlux(RemoteData.Room.class);
    }

    private static String join(Collection<Long> ids) {
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
}
