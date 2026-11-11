package com.clinic.reminder.log;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReminderLogRepository extends JpaRepository<ReminderLog, Long> {

    Optional<ReminderLog> findByAppointmentIdAndReminderType(Long appointmentId, ReminderType reminderType);

    List<ReminderLog> findByAppointmentIdInAndReminderType(Collection<Long> appointmentIds, ReminderType reminderType);

    List<ReminderLog> findTop100ByOrderByCreatedAtDesc();

    List<ReminderLog> findTop100ByStatusOrderByCreatedAtDesc(ReminderStatus status);

    List<ReminderLog> findTop100ByReminderTypeOrderByCreatedAtDesc(ReminderType reminderType);

    List<ReminderLog> findTop100ByStatusAndReminderTypeOrderByCreatedAtDesc(ReminderStatus status, ReminderType reminderType);

    @Query("SELECT r FROM ReminderLog r WHERE " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:type IS NULL OR r.reminderType = :type) " +
           "ORDER BY r.createdAt DESC")
    List<ReminderLog> findLogs(@Param("status") ReminderStatus status,
                               @Param("type") ReminderType type,
                               Pageable pageable);
}
