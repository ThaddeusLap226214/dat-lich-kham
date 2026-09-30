package com.clinic.scheduling.appointment;

import com.clinic.scheduling.appointment.dto.AppointmentResponse;
import com.clinic.scheduling.appointment.dto.CancelRequest;
import com.clinic.scheduling.appointment.dto.HoldRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping("/hold")
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse hold(@Valid @RequestBody HoldRequest request,
                                    @RequestHeader("X-User-Id") Long accountId) {
        return appointmentService.hold(request, accountId);
    }

    @PostMapping("/{id}/confirm")
    public AppointmentResponse confirm(@PathVariable Long id, @RequestHeader("X-User-Id") Long accountId) {
        return appointmentService.confirm(id, accountId);
    }

    @PostMapping("/{id}/cancel")
    public AppointmentResponse cancel(@PathVariable Long id, @Valid @RequestBody CancelRequest request,
                                      @RequestHeader("X-User-Id") Long accountId,
                                      @RequestHeader("X-User-Role") String role) {
        return appointmentService.cancel(id, request.reason(), accountId, role);
    }

    @PostMapping("/{id}/complete")
    public AppointmentResponse complete(@PathVariable Long id,
                                        @RequestHeader(value = "X-Doctor-Id", required = false) Long doctorId,
                                        @RequestHeader("X-User-Role") String role) {
        return appointmentService.complete(id, doctorId, role);
    }

    @GetMapping("/mine")
    public List<AppointmentResponse> mine(@RequestHeader("X-User-Id") Long accountId) {
        return appointmentService.listMine(accountId);
    }

    @GetMapping
    public List<AppointmentResponse> listByDoctor(@RequestParam Long doctorId,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                  @RequestParam(required = false) AppointmentStatus status) {
        return appointmentService.listByDoctor(doctorId, date, status);
    }

    @GetMapping("/{id}")
    public AppointmentResponse get(@PathVariable Long id) {
        return appointmentService.get(id);
    }
}
