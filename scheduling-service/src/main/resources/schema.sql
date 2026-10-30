-- scheduling_db: chỉ Scheduling Service được đọc/ghi
-- patient_id, doctor_id, room_id, booked_by_account_id là mã của service khác: lưu giá trị thường, KHÔNG có khóa ngoại
CREATE TABLE IF NOT EXISTS appointments (
    id                      BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    patient_id              BIGINT      NOT NULL,
    doctor_id               BIGINT      NOT NULL,
    room_id                 BIGINT,
    work_schedule_id        BIGINT,
    booked_by_account_id    BIGINT      NOT NULL,
    start_time              DATETIME    NOT NULL,
    end_time                DATETIME    NOT NULL,
    status                  VARCHAR(12) NOT NULL,
    hold_expires_at         DATETIME,
    reason                  VARCHAR(500),
    cancel_reason           VARCHAR(500),
    cancelled_by_account_id BIGINT,
    confirmed_at            DATETIME,
    cancelled_at            DATETIME,
    completed_at            DATETIME,
    no_show_at              DATETIME,
    created_at             DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version                 BIGINT      NOT NULL DEFAULT 0,
    -- Chống đặt trùng: lịch còn hiệu lực (HELD/CONFIRMED/COMPLETED/NO_SHOW) có khóa "bác sĩ@giờ bắt đầu";
    -- ràng buộc UNIQUE bảo đảm hai người cùng giữ một slot thì chỉ một lệnh INSERT thành công.
    -- Lịch EXPIRED/CANCELLED có khóa NULL nên slot được nhả ra cho người khác.
    slot_lock               VARCHAR(50) GENERATED ALWAYS AS (
        IF(status IN ('HELD', 'CONFIRMED', 'COMPLETED', 'NO_SHOW'), CONCAT(doctor_id, '@', start_time), NULL)) STORED,
    CONSTRAINT uk_appointments_slot_lock UNIQUE (slot_lock),
    CONSTRAINT ck_appointments_status CHECK (status IN ('HELD', 'CONFIRMED', 'COMPLETED', 'NO_SHOW', 'EXPIRED', 'CANCELLED')),
    CONSTRAINT ck_appointments_time CHECK (start_time < end_time),
    INDEX idx_appointments_patient (patient_id, start_time),
    INDEX idx_appointments_doctor (doctor_id, start_time),
    INDEX idx_appointments_status (status, start_time),
    INDEX idx_appointments_booked_by (booked_by_account_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Lịch hẹn đã chuyển sang hồ sơ khác khi Patient Service gộp hồ sơ; dùng để hoàn lại khi tách
CREATE TABLE IF NOT EXISTS patient_reassignments (
    id              BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    appointment_id  BIGINT      NOT NULL,
    from_patient_id BIGINT      NOT NULL,
    to_patient_id   BIGINT      NOT NULL,
    merge_event_id  VARCHAR(36) NOT NULL,
    reassigned_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reverted_at     DATETIME,
    CONSTRAINT fk_patient_reassignments_appointment FOREIGN KEY (appointment_id) REFERENCES appointments (id),
    INDEX idx_patient_reassignments_event (merge_event_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Sự kiện RabbitMQ đã xử lý: nhận lại cùng event_id thì bỏ qua (chống xử lý lặp)
CREATE TABLE IF NOT EXISTS processed_events (
    event_id     VARCHAR(36) NOT NULL PRIMARY KEY,
    event_type   VARCHAR(50) NOT NULL,
    processed_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
