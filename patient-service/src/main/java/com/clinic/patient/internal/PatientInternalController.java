package com.clinic.patient.internal;

import com.clinic.patient.patient.PatientService;
import com.clinic.patient.patient.dto.PatientSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// API nội bộ: chỉ service khác gọi (Gateway không mở đường /internal ra ngoài)
@RestController
@RequestMapping("/internal/v1")
public class PatientInternalController {

    private final PatientService patientService;

    public PatientInternalController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping("/patients")
    public List<PatientSummary> patientsByIds(@RequestParam List<Long> ids) {
        return patientService.findSummaries(ids);
    }
}
