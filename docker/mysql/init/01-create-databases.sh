#!/bin/bash
# Chạy một lần khi volume MySQL còn trống: tạo 6 database (5 service + Keycloak), mỗi bên một tài khoản chỉ có quyền trên database của mình
set -euo pipefail

create_db() {
  local db="$1" user="$2" pass="$3"
  mysql --protocol=socket -uroot -p"${MYSQL_ROOT_PASSWORD}" <<SQL
CREATE DATABASE IF NOT EXISTS \`${db}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '${user}'@'%' IDENTIFIED BY '${pass}';
GRANT ALL PRIVILEGES ON \`${db}\`.* TO '${user}'@'%';
SQL
}

create_db auth_db auth_user "${AUTH_DB_PASSWORD}"
create_db provider_db provider_user "${PROVIDER_DB_PASSWORD}"
create_db patient_db patient_user "${PATIENT_DB_PASSWORD}"
create_db scheduling_db scheduling_user "${SCHEDULING_DB_PASSWORD}"
create_db reminder_db reminder_user "${REMINDER_DB_PASSWORD}"
create_db keycloak_db keycloak_user "${KEYCLOAK_DB_PASSWORD}"
