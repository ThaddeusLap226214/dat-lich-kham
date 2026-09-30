package com.clinic.patient.patient;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "patients")
@Getter
@Setter
@NoArgsConstructor
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tài khoản PATIENT quản lý hồ sơ (mã bên Auth Service, không có khóa ngoại); null nếu lễ tân tạo tại quầy
    @Column(name = "owner_account_id")
    private Long ownerAccountId;

    @Column(name = "created_by_account_id", nullable = false)
    private Long createdByAccountId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    // Họ tên đã bỏ dấu, viết thường, gộp khoảng trắng; dùng để so khớp hồ sơ trùng
    @Column(name = "normalized_name", nullable = false)
    private String normalizedName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;

    @Column(length = 20)
    private String phone;

    private String email;

    @Column(length = 500)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "id_type", length = 10)
    private IdType idType;

    @Column(name = "id_number", length = 20)
    private String idNumber;

    @Column(name = "guardian_name")
    private String guardianName;

    @Column(name = "guardian_phone", length = 20)
    private String guardianPhone;

    @Column(name = "guardian_relation", length = 50)
    private String guardianRelation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PatientStatus status;

    @Column(name = "merged_into_id")
    private Long mergedIntoId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Khóa lạc quan: hai lễ tân cùng sửa một hồ sơ thì người lưu sau nhận lỗi thay vì ghi đè
    @Version
    private Long version;
}
