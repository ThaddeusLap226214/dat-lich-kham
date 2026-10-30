-- auth_db: chỉ Auth Service được đọc/ghi
CREATE TABLE IF NOT EXISTS accounts (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    -- Mã người dùng bên Keycloak (UUID); mật khẩu chỉ nằm ở Keycloak, bảng này không lưu
    keycloak_user_id VARCHAR(36),
    full_name     VARCHAR(255) NOT NULL,
    email         VARCHAR(255),
    phone         VARCHAR(20),
    role          VARCHAR(20)  NOT NULL,
    doctor_id     BIGINT,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_accounts_username UNIQUE (username),
    CONSTRAINT uk_accounts_keycloak_user UNIQUE (keycloak_user_id),
    CONSTRAINT uk_accounts_email UNIQUE (email),
    CONSTRAINT ck_accounts_role CHECK (role IN ('PATIENT', 'RECEPTIONIST', 'DOCTOR', 'ADMIN'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
