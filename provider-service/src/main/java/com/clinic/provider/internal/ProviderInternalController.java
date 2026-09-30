package com.clinic.provider.internal;

import com.clinic.provider.catalog.CatalogService;
import com.clinic.provider.catalog.dto.DoctorResponse;
import com.clinic.provider.catalog.dto.RoomResponse;
import com.clinic.provider.schedule.WorkScheduleService;
import com.clinic.provider.schedule.dto.WorkingHoursResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

// API nội bộ: chỉ service khác gọi (Gateway không mở đường /internal ra ngoài)
@RestController
@RequestMapping("/internal/v1")
public class ProviderInternalController {

    private final CatalogService catalogService;
    private final WorkScheduleService workScheduleService;

    public ProviderInternalController(CatalogService catalogService, WorkScheduleService workScheduleService) {
        this.catalogService = catalogService;
        this.workScheduleService = workScheduleService;
    }

    @GetMapping("/doctors")
    public List<DoctorResponse> doctorsByIds(@RequestParam List<Long> ids) {
        return catalogService.findDoctorsByIds(ids);
    }

    @GetMapping("/rooms")
    public List<RoomResponse> roomsByIds(@RequestParam List<Long> ids) {
        return catalogService.findRoomsByIds(ids);
    }

    @GetMapping("/doctors/{id}/working-hours")
    public WorkingHoursResponse workingHours(@PathVariable Long id,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return workScheduleService.getWorkingHours(id, date);
    }
}
