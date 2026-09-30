package com.clinic.reminder.log;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReminderLogRepository extends JpaRepository<ReminderLog, Long> {

    Optional<ReminderLog> findByAppointmentIdAndReminderType(Long appointmentId, ReminderType reminderType);

    List<ReminderLog> findByAppointmentIdInAndReminderType(Collection<Long> appointmentIds, ReminderType reminderType);

    List<ReminderLog> findTop100ByOrderByCreatedAtDesc();

    List<ReminderLog> findTop100ByStatusOrderByCreatedAtDesc(ReminderStatus status);
}
