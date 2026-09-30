-- provider_db: chỉ Provider Service được đọc/ghi
CREATE TABLE IF NOT EXISTS specialties (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(20)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_specialties_code UNIQUE (code),
    CONSTRAINT uk_specialties_name UNIQUE (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS rooms (
    id       BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code     VARCHAR(20)  NOT NULL,
    name     VARCHAR(100) NOT NULL,
    location VARCHAR(100),
    active   BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_rooms_code UNIQUE (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS doctors (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    full_name    VARCHAR(255) NOT NULL,
    title        VARCHAR(50),
    specialty_id BIGINT       NOT NULL,
    phone        VARCHAR(20),
    email        VARCHAR(255),
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_doctors_specialty FOREIGN KEY (specialty_id) REFERENCES specialties (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Lịch làm việc lặp lại hằng tuần; day_of_week theo ISO: 1 = Thứ Hai ... 7 = Chủ nhật (khớp java.time.DayOfWeek)
CREATE TABLE IF NOT EXISTS work_schedules (
    id             BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    doctor_id      BIGINT   NOT NULL,
    room_id        BIGINT   NOT NULL,
    day_of_week    TINYINT  NOT NULL,
    start_time     TIME     NOT NULL,
    end_time       TIME     NOT NULL,
    slot_minutes   INT      NOT NULL,
    effective_from DATE     NOT NULL,
    effective_to   DATE,
    active         BOOLEAN  NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_work_schedules_doctor FOREIGN KEY (doctor_id) REFERENCES doctors (id),
    CONSTRAINT fk_work_schedules_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT ck_work_schedules_day CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT ck_work_schedules_time CHECK (start_time < end_time),
    CONSTRAINT ck_work_schedules_slot CHECK (slot_minutes BETWEEN 5 AND 120),
    INDEX idx_work_schedules_doctor_day (doctor_id, day_of_week)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
