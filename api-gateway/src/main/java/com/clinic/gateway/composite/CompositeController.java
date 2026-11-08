package com.clinic.gateway.composite;

import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

@RestController
@RequestMapping("/api/v1/composite")
public class CompositeController {

    private final SchedulingClient schedulingClient;
    private final ProviderClient providerClient;
    private final PatientClient patientClient;
    private final ReactiveCircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public CompositeController(SchedulingClient schedulingClient, ProviderClient providerClient,
                               PatientClient patientClient, ReactiveCircuitBreakerFactory<?, ?> circuitBreakerFactory) {
        this.schedulingClient = schedulingClient;
        this.providerClient = providerClient;
        this.patientClient = patientClient;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    @GetMapping("/my-appointments")
    public Mono<List<AppointmentView>> myAppointments(@RequestHeader("X-User-Id") Long accountId) {
        return myPatientIds(accountId)
            .flatMap(patientIds -> fromScheduling("scheduling-by-account",
                schedulingClient.findByAccount(accountId, patientIds)))
            .flatMap(this::compose);
    }

    @GetMapping("/doctor-agenda")
    public Mono<List<AppointmentView>> doctorAgenda(@RequestParam Long doctorId,
                                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return fromScheduling("scheduling-by-doctor", schedulingClient.findByDoctorAndDate(doctorId, date))
            .flatMap(this::compose);
    }

    // Bước 1: Patient lỗi thì coi như không có hồ sơ nào, chỉ lấy lịch do chính tài khoản đặt
    private Mono<List<Long>> myPatientIds(Long accountId) {
        Mono<List<Long>> call = patientClient.findMine(accountId).map(RemoteData.Patient::id).distinct().collectList();
        return breaker("patient-mine").run(call, error -> Mono.just(List.<Long>of()));
    }

    // Bước 2: Scheduling là nguồn chính, lỗi thì cả API trả 503
    private Mono<List<RemoteData.Appointment>> fromScheduling(String breakerName, Flux<RemoteData.Appointment> call) {
        return breaker(breakerName).run(call.collectList(), error -> Mono.error(
            new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Scheduling không phản hồi")));
    }

    // Bước 3 đến 5: gom mã, gọi song song, ghép; nguồn phụ lỗi thì phần đó để null
    private Mono<List<AppointmentView>> compose(List<RemoteData.Appointment> appointments) {
        if (appointments.isEmpty()) {
            return Mono.just(List.of());
        }
        Set<Long> doctorIds = distinct(appointments, RemoteData.Appointment::doctorId);
        Set<Long> roomIds = distinct(appointments, RemoteData.Appointment::roomId);
        Set<Long> patientIds = distinct(appointments, RemoteData.Appointment::patientId);

        return Mono.zip(
            lookup("provider-doctors", doctorIds, providerClient::findDoctors, RemoteData.Doctor::id),
            lookup("provider-rooms", roomIds, providerClient::findRooms, RemoteData.Room::id),
            lookup("patient-lookup", patientIds, patientClient::findPatients, RemoteData.Patient::id))
            .map(tuple -> appointments.stream()
                .map(a -> toView(a, tuple.getT1().get(a.doctorId()), tuple.getT2().get(a.roomId()),
                    tuple.getT3().get(a.patientId())))
                .toList());
    }

    private <T> Mono<Map<Long, T>> lookup(String breakerName, Set<Long> ids,
                                          Function<Collection<Long>, Flux<T>> call, Function<T, Long> idOf) {
        if (ids.isEmpty()) {
            return Mono.just(Map.of());
        }
        Mono<Map<Long, T>> byId = call.apply(ids).collectMap(idOf);
        return breaker(breakerName).run(byId, error -> Mono.just(Map.<Long, T>of()));
    }

    private static AppointmentView toView(RemoteData.Appointment a, RemoteData.Doctor doctor, RemoteData.Room room,
                                          RemoteData.Patient patient) {
        return new AppointmentView(a.id(), a.status(), a.startTime(), a.endTime(), a.reason(), a.cancelReason(),
            doctor == null ? null : new AppointmentView.DoctorView(doctor.id(), doctor.fullName(), doctor.title(),
                doctor.specialtyName()),
            room == null ? null : new AppointmentView.RoomView(room.id(), room.code(), room.name()),
            patient == null ? null : new AppointmentView.PatientView(patient.id(), patient.fullName(),
                patient.dateOfBirth() == null ? null : patient.dateOfBirth().toString()));
    }

    private static Set<Long> distinct(List<RemoteData.Appointment> appointments,
                                      Function<RemoteData.Appointment, Long> idOf) {
        Set<Long> ids = new LinkedHashSet<>();
        appointments.stream().map(idOf).filter(Objects::nonNull).forEach(ids::add);
        return ids;
    }

    private ReactiveCircuitBreaker breaker(String name) {
        return circuitBreakerFactory.create(name);
    }
}
