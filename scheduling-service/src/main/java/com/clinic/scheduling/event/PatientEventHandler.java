package com.clinic.scheduling.event;

import com.clinic.scheduling.appointment.Appointment;
import com.clinic.scheduling.appointment.AppointmentRepository;
import com.clinic.scheduling.reassignment.PatientReassignment;
import com.clinic.scheduling.reassignment.PatientReassignmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PatientEventHandler {

    private static final Logger log = LoggerFactory.getLogger(PatientEventHandler.class);

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

    @Transactional
    public void handle(PatientEvent event) {
        // Kiểm tra lũy đẳng (Idempotency): bỏ qua nếu eventId đã có trong processed_events
        if (processedEventRepository.existsById(event.eventId())) {
            log.info("Sự kiện {} eventId={} đã được xử lý trước đó, bỏ qua", event.eventType(), event.eventId());
            return;
        }

        if (event.eventType() == PatientEvent.PatientEventType.PATIENT_MERGED) {
            handlePatientMerged(event);
        } else if (event.eventType() == PatientEvent.PatientEventType.PATIENT_UNMERGED) {
            handlePatientUnmerged(event);
        }

        // Đánh dấu sự kiện đã xử lý
        ProcessedEvent processedEvent = new ProcessedEvent();
        processedEvent.setEventId(event.eventId());
        processedEvent.setEventType(event.eventType().name());
        processedEventRepository.save(processedEvent);
    }

    private void handlePatientMerged(PatientEvent event) {
        Long sourceId = event.sourcePatientId();
        Long targetId = event.targetPatientId();

        // CS14: Chuyển mọi lịch của hồ sơ bị gộp (mọi trạng thái) sang hồ sơ giữ lại
        List<Appointment> sourceAppointments = appointmentRepository.findByPatientId(sourceId);
        log.info("Gộp hồ sơ: chuyển {} lịch từ hồ sơ {} sang hồ sơ {}", sourceAppointments.size(), sourceId, targetId);

        for (Appointment appointment : sourceAppointments) {
            appointment.setPatientId(targetId);
            appointmentRepository.save(appointment);

            // Ghi nhật ký chuyển lịch để hoàn lại khi tách
            PatientReassignment reassignment = new PatientReassignment();
            reassignment.setAppointmentId(appointment.getId());
            reassignment.setFromPatientId(sourceId);
            reassignment.setToPatientId(targetId);
            reassignment.setMergeEventId(event.eventId());
            reassignmentRepository.save(reassignment);
        }
    }

    private void handlePatientUnmerged(PatientEvent event) {
        String mergeEventId = event.relatedMergeEventId();
        LocalDateTime now = LocalDateTime.now();

        // Tìm các bản ghi chuyển lịch theo relatedMergeEventId chưa được hoàn lại
        List<PatientReassignment> reassignments = reassignmentRepository.findByMergeEventIdAndRevertedAtIsNull(mergeEventId);
        log.info("Tách hồ sơ: hoàn lại {} lịch đã chuyển theo mergeEventId={}", reassignments.size(), mergeEventId);

        for (PatientReassignment reassignment : reassignments) {
            appointmentRepository.findById(reassignment.getAppointmentId()).ifPresent(appointment -> {
                appointment.setPatientId(reassignment.getFromPatientId());
                appointmentRepository.save(appointment);
            });

            reassignment.setRevertedAt(now);
            reassignmentRepository.save(reassignment);
        }
    }
}
