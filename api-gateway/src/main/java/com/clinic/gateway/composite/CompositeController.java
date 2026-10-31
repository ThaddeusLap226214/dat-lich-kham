package com.clinic.gateway.composite;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/composite")
public class CompositeController {

    private final SchedulingClient schedulingClient;
    private final ProviderClient providerClient;
    private final PatientClient patientClient;

    public CompositeController(SchedulingClient schedulingClient, ProviderClient providerClient, PatientClient patientClient) {
        this.schedulingClient = schedulingClient;
        this.providerClient = providerClient;
        this.patientClient = patientClient;
    }

    @GetMapping("/my-appointments")
    public Mono<List<AppointmentView>> myAppointments(@RequestHeader("X-User-Id") Long accountId) {
        return Mono.error(new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
            "Chưa làm: ghép Scheduling + Provider + Patient (docs/03-hop-dong-api.md, mục B6)"));
    }

    @GetMapping("/doctor-agenda")
    public Mono<List<AppointmentView>> doctorAgenda(@RequestParam Long doctorId, @RequestParam LocalDate date) {
        return Mono.error(new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
            "Chưa làm: lịch khám trong ngày của bác sĩ kèm tên bệnh nhân"));
    }
}
