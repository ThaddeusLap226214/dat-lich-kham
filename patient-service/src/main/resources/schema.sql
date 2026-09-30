-- patient_db: chỉ Patient Service được đọc/ghi
CREATE TABLE IF NOT EXISTS patients (
    id                    BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    owner_account_id      BIGINT,
    created_by_account_id BIGINT       NOT NULL,
    full_name             VARCHAR(255) NOT NULL,
    normalized_name       VARCHAR(255) NOT NULL,
    date_of_birth         DATE         NOT NULL,
    gender                VARCHAR(10)  NOT NULL,
    phone                 VARCHAR(20),
    email                 VARCHAR(255),
    address               VARCHAR(500),
    id_type               VARCHAR(10),
    id_number             VARCHAR(20),
    guardian_name         VARCHAR(255),
    guardian_phone        VARCHAR(20),
    guardian_relation     VARCHAR(50),
    status                VARCHAR(10)  NOT NULL,
    merged_into_id        BIGINT,
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version               BIGINT       NOT NULL DEFAULT 0,
    -- Cột tự tính: chỉ hồ sơ ACTIVE mới "chiếm" giấy tờ; hồ sơ TEMPORARY được phép mang giấy tờ trùng để chờ lễ tân duyệt
    active_identity       VARCHAR(40) GENERATED ALWAYS AS (
        IF(status = 'ACTIVE' AND id_number IS NOT NULL, CONCAT(id_type, ':', id_number), NULL)) STORED,
    CONSTRAINT uk_patients_active_identity UNIQUE (active_identity),
    CONSTRAINT fk_patients_merged_into FOREIGN KEY (merged_into_id) REFERENCES patients (id),
    CONSTRAINT ck_patients_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    CONSTRAINT ck_patients_status CHECK (status IN ('TEMPORARY', 'ACTIVE', 'MERGED')),
    CONSTRAINT ck_patients_id_type CHECK (id_type IS NULL OR id_type IN ('CCCD', 'PASSPORT', 'BHYT')),
    INDEX idx_patients_match (normalized_name, date_of_birth),
    INDEX idx_patients_identity (id_type, id_number),
    INDEX idx_patients_owner (owner_account_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Hàng chờ duyệt: mỗi dòng là một cặp hồ sơ nghi là cùng một người
CREATE TABLE IF NOT EXISTS duplicate_candidates (
    id                     BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    patient_id             BIGINT       NOT NULL,
    candidate_patient_id   BIGINT       NOT NULL,
    reason                 VARCHAR(40)  NOT NULL,
    score                  DECIMAL(5, 2),
    status                 VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    reviewed_by_account_id BIGINT,
    reviewed_at            DATETIME,
    review_note            VARCHAR(500),
    created_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_duplicate_candidates_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_duplicate_candidates_candidate FOREIGN KEY (candidate_patient_id) REFERENCES patients (id),
    CONSTRAINT uk_duplicate_candidates_pair UNIQUE (patient_id, candidate_patient_id),
    CONSTRAINT ck_duplicate_candidates_reason CHECK (reason IN ('SAME_ID_DIFFERENT_INFO', 'SAME_NAME_DOB_PHONE', 'ID_ADDED_BELONGS_TO_OTHER')),
    CONSTRAINT ck_duplicate_candidates_status CHECK (status IN ('PENDING', 'MERGED', 'NOT_DUPLICATE')),
    INDEX idx_duplicate_candidates_status (status, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Lịch sử gộp / tách; mỗi dòng ứng với một sự kiện đã phát qua RabbitMQ (event_id)
CREATE TABLE IF NOT EXISTS merge_history (
    id                      BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    action                  VARCHAR(10) NOT NULL,
    source_patient_id       BIGINT      NOT NULL,
    target_patient_id       BIGINT      NOT NULL,
    source_previous_status  VARCHAR(10) NOT NULL,
    duplicate_candidate_id  BIGINT,
    related_merge_id        BIGINT,
    event_id                VARCHAR(36) NOT NULL,
    performed_by_account_id BIGINT      NOT NULL,
    performed_at            DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    note                    VARCHAR(500),
    CONSTRAINT fk_merge_history_source FOREIGN KEY (source_patient_id) REFERENCES patients (id),
    CONSTRAINT fk_merge_history_target FOREIGN KEY (target_patient_id) REFERENCES patients (id),
    CONSTRAINT fk_merge_history_candidate FOREIGN KEY (duplicate_candidate_id) REFERENCES duplicate_candidates (id),
    CONSTRAINT fk_merge_history_related FOREIGN KEY (related_merge_id) REFERENCES merge_history (id),
    CONSTRAINT uk_merge_history_event UNIQUE (event_id),
    CONSTRAINT ck_merge_history_action CHECK (action IN ('MERGE', 'UNMERGE'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
