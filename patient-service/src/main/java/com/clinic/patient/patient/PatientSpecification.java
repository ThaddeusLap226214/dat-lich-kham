package com.clinic.patient.patient;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Chuyển từ PatientSpecification (repo MPI của Như), rút lại theo bảng patients mới
public final class PatientSpecification {

    private PatientSpecification() {
    }

    // Một ô tìm kiếm khớp họ tên (có dấu hoặc không dấu), số giấy tờ hoặc số điện thoại
    public static Specification<Patient> search(String keyword, PatientStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                String text = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                String normalized = "%" + NameNormalizer.normalize(keyword) + "%";
                String compact = "%" + keyword.replaceAll("\\s+", "") + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("fullName")), text),
                    cb.like(root.get("normalizedName"), normalized),
                    cb.like(root.get("idNumber"), compact),
                    cb.like(root.get("phone"), compact)));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
