package com.clinic.provider.catalog;

import com.clinic.provider.catalog.dto.DoctorRequest;
import com.clinic.provider.catalog.dto.DoctorResponse;
import com.clinic.provider.schedule.WorkScheduleService;
import com.clinic.provider.schedule.dto.WorkScheduleRequest;
import com.clinic.provider.schedule.dto.WorkScheduleResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/doctors")
public class DoctorController {

    private final CatalogService catalogService;
    private final WorkScheduleService workScheduleService;

    public DoctorController(CatalogService catalogService, WorkScheduleService workScheduleService) {
        this.catalogService = catalogService;
        this.workScheduleService = workScheduleService;
    }

    @GetMapping
    public List<DoctorResponse> list(@RequestParam(required = false) Long specialtyId) {
        return catalogService.listDoctors(specialtyId);
    }

    @GetMapping("/{id}")
    public DoctorResponse get(@PathVariable Long id) {
        return catalogService.getDoctor(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DoctorResponse create(@Valid @RequestBody DoctorRequest request) {
        return catalogService.createDoctor(request);
    }

    @PutMapping("/{id}")
    public DoctorResponse update(@PathVariable Long id, @Valid @RequestBody DoctorRequest request) {
        return catalogService.updateDoctor(id, request);
    }

    @GetMapping("/{id}/work-schedules")
    public List<WorkScheduleResponse> listSchedules(@PathVariable Long id) {
        return workScheduleService.listByDoctor(id);
    }

    @PostMapping("/{id}/work-schedules")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkScheduleResponse addSchedule(@PathVariable Long id, @Valid @RequestBody WorkScheduleRequest request) {
        return workScheduleService.create(id, request);
    }
}
