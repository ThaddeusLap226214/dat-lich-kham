package com.clinic.gateway.composite;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

@Component
public class SchedulingClient {

    private final WebClient.Builder webClientBuilder;

    public SchedulingClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public Flux<RemoteData.Appointment> findByAccount(Long accountId) {
        return webClientBuilder.build().get()
            .uri("http://scheduling-service/internal/v1/appointments?bookedByAccountId={id}", accountId)
            .retrieve()
            .bodyToFlux(RemoteData.Appointment.class);
    }

    public Flux<RemoteData.Appointment> findByDoctorAndDate(Long doctorId, LocalDate date) {
        return webClientBuilder.build().get()
            .uri("http://scheduling-service/internal/v1/appointments?doctorId={id}&from={from}&to={to}",
                doctorId, date.atStartOfDay(), date.plusDays(1).atStartOfDay())
            .retrieve()
            .bodyToFlux(RemoteData.Appointment.class);
    }
}
