package com.clinic.provider.catalog;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    @EntityGraph(attributePaths = "specialty")
    List<Doctor> findByActiveTrue();

    @EntityGraph(attributePaths = "specialty")
    List<Doctor> findBySpecialtyIdAndActiveTrue(Long specialtyId);

    @EntityGraph(attributePaths = "specialty")
    List<Doctor> findByIdIn(Collection<Long> ids);
}
