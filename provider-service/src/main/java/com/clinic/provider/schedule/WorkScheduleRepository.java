package com.clinic.provider.schedule;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, Long> {

    @EntityGraph(attributePaths = "room")
    List<WorkSchedule> findByDoctorIdOrderByDayOfWeekAscStartTimeAsc(Long doctorId);

    // Các ca của bác sĩ còn hiệu lực vào ngày date (date rơi vào thứ dayOfWeek)
    @EntityGraph(attributePaths = "room")
    @Query("""
        select w from WorkSchedule w
        where w.doctor.id = :doctorId and w.dayOfWeek = :dayOfWeek and w.active = true
          and w.effectiveFrom <= :date and (w.effectiveTo is null or w.effectiveTo >= :date)
        order by w.startTime
        """)
    List<WorkSchedule> findEffective(@Param("doctorId") Long doctorId, @Param("dayOfWeek") int dayOfWeek,
                                     @Param("date") LocalDate date);
}
