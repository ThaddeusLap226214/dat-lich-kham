package com.clinic.scheduling.event;

import com.clinic.scheduling.appointment.AppointmentRepository;
import com.clinic.scheduling.common.NotImplementedYetException;
import com.clinic.scheduling.reassignment.PatientReassignmentRepository;
import org.springframework.stereotype.Service;

@Service
public class PatientEventHandler {

    private final ProcessedEventRepository processedEventRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientReassignmentRepository reassignmentRepository;

    public PatientEventHandler(ProcessedEventRepository processedEventRepository,
                               AppointmentRepository appointmentRepository,
                               PatientReassignmentRepository reassignmentRepository) {
        this.processedEventRepository = processedEventRepository;
        this.appointmentRepository = appointmentRepository;
        this.reassignmentRepository = reassignmentRepository;
    }

    public void handle(PatientEvent event) {
        throw new NotImplementedYetException(
            "bỏ qua nếu eventId đã có trong processed_events; MERGED: chuyển lịch HELD/CONFIRMED của source sang target "
                + "và ghi patient_reassignments; UNMERGED: hoàn lại theo relatedMergeEventId");
    }
}
