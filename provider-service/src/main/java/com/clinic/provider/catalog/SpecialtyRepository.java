package com.clinic.provider.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpecialtyRepository extends JpaRepository<Specialty, Long> {

    List<Specialty> findByActiveTrueOrderByNameAsc();

    boolean existsByCode(String code);
}
