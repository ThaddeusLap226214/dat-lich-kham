package com.clinic.scheduling.appointment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // Các lịch còn chiếm slot của bác sĩ trong khoảng thời gian (dùng khi tính slot trống)
    List<Appointment> findByDoctorIdAndStatusInAndStartTimeBetween(Long doctorId, Collection<AppointmentStatus> statuses,
                                                                    LocalDateTime from, LocalDateTime to);

    // Lịch của bệnh nhân bị chồng giờ với [start, end) — dùng cho quy tắc "một bệnh nhân không đặt hai lịch trùng giờ"
    @Query("""
        select a from Appointment a
        where a.patientId = :patientId and a.status in :statuses
          and a.startTime < :end and a.endTime > :start
        """)
    List<Appointment> findOverlappingForPatient(@Param("patientId") Long patientId,
                                                @Param("statuses") Collection<AppointmentStatus> statuses,
                                                @Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);

    List<Appointment> findByBookedByAccountIdOrderByStartTimeDesc(Long bookedByAccountId);

    List<Appointment> findByDoctorIdAndStartTimeBetweenOrderByStartTime(Long doctorId, LocalDateTime from,
                                                                        LocalDateTime to);

    List<Appointment> findByStatusAndStartTimeBetweenOrderByStartTime(AppointmentStatus status, LocalDateTime from,
                                                                      LocalDateTime to);

    List<Appointment> findByPatientIdAndStatusIn(Long patientId, Collection<AppointmentStatus> statuses);

    @Modifying
    @Query("""
        update Appointment a set a.status = com.clinic.scheduling.appointment.AppointmentStatus.EXPIRED
        where a.status = com.clinic.scheduling.appointment.AppointmentStatus.HELD and a.holdExpiresAt < :now
        """)
    int expireHolds(@Param("now") LocalDateTime now);
}
