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
    -- Nhắc trước giờ khám (cron), xác nhận khi đặt và báo hủy (theo sự kiện từ Scheduling);
    -- BEFORE_APPOINTMENT_2H dành cho nhắc lần hai (việc mở rộng P5-7)
    CONSTRAINT ck_reminder_logs_type CHECK (reminder_type IN ('BEFORE_APPOINTMENT', 'BEFORE_APPOINTMENT_2H',
                                                              'CONFIRMATION', 'CANCELLATION')),
    INDEX idx_reminder_logs_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Mẫu thư cho từng loại; chỗ điền {patientName}, {startTime}, {appointmentId}, {cancelReason}
-- updated_by_account_id là mã bên Auth Service, không khóa ngoại; NULL = mẫu nạp sẵn
CREATE TABLE IF NOT EXISTS message_templates (
    type                  VARCHAR(30)  NOT NULL PRIMARY KEY,
    subject               VARCHAR(255) NOT NULL,
    body                  TEXT         NOT NULL,
    updated_by_account_id BIGINT,
    updated_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT ck_message_templates_type CHECK (type IN ('BEFORE_APPOINTMENT', 'BEFORE_APPOINTMENT_2H',
                                                         'CONFIRMATION', 'CANCELLATION'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Sự kiện lịch hẹn (appointment.events) đã xử lý: nhận lại cùng event_id thì bỏ qua (chống gửi thư lặp)
CREATE TABLE IF NOT EXISTS processed_events (
    event_id     VARCHAR(36) NOT NULL PRIMARY KEY,
    event_type   VARCHAR(50) NOT NULL,
    processed_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
