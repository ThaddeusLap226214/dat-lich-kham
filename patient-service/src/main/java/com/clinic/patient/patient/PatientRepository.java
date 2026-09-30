package com.clinic.patient.patient;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long>, JpaSpecificationExecutor<Patient> {

    Optional<Patient> findByIdTypeAndIdNumberAndStatus(IdType idType, String idNumber, PatientStatus status);

    List<Patient> findByNormalizedNameAndDateOfBirthAndStatusNot(String normalizedName, LocalDate dateOfBirth,
                                                                 PatientStatus status);

    List<Patient> findByOwnerAccountIdAndStatusNot(Long ownerAccountId, PatientStatus status);

    List<Patient> findByIdIn(Collection<Long> ids);
}
