package com.clinic.scheduling.internal;

import com.clinic.scheduling.appointment.AppointmentService;
import com.clinic.scheduling.appointment.AppointmentStatus;
import com.clinic.scheduling.appointment.dto.AppointmentResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

// API nội bộ: chỉ service khác gọi (Gateway không mở đường /internal ra ngoài)
@RestController
@RequestMapping("/internal/v1")
public class SchedulingInternalController {

    private final AppointmentService appointmentService;

    public SchedulingInternalController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping("/appointments")
    public List<AppointmentResponse> search(
        @RequestParam(required = false) Long bookedByAccountId,
        @RequestParam(required = false) Long doctorId,
        @RequestParam(required = false) AppointmentStatus status,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return appointmentService.search(bookedByAccountId, doctorId, status, from, to);
    }
}
