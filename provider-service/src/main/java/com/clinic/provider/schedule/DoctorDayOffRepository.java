package com.clinic.provider.schedule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DoctorDayOffRepository extends JpaRepository<DoctorDayOff, Long> {

    List<DoctorDayOff> findByDoctorIdOrderByOffDateAsc(Long doctorId);

    boolean existsByDoctorIdAndOffDate(Long doctorId, LocalDate offDate);

    Optional<DoctorDayOff> findByIdAndDoctorId(Long id, Long doctorId);
}

