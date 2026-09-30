-- reminder_db: chỉ Reminder Service được đọc/ghi
-- appointment_id, patient_id là mã bên Scheduling / Patient Service: giá trị thường, KHÔNG có khóa ngoại
CREATE TABLE IF NOT EXISTS reminder_logs (
    id                BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    appointment_id    BIGINT       NOT NULL,
    patient_id        BIGINT       NOT NULL,
    appointment_start DATETIME     NOT NULL,
    reminder_type     VARCHAR(30)  NOT NULL,
    recipient_email   VARCHAR(255),
    status            VARCHAR(10)  NOT NULL,
    attempt_count     INT          NOT NULL DEFAULT 1,
    last_error        VARCHAR(1000),
    sent_at           DATETIME,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    -- Mỗi lịch hẹn chỉ có một dòng cho mỗi loại nhắc → cron chạy lại không gửi lặp
    CONSTRAINT uk_reminder_logs_appointment UNIQUE (appointment_id, reminder_type),
    CONSTRAINT ck_reminder_logs_status CHECK (status IN ('SENT', 'FAILED', 'SKIPPED')),
    INDEX idx_reminder_logs_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
