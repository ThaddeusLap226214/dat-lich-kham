package com.clinic.auth.keycloak;

// Thông tin cần để tạo một người dùng bên Keycloak; accountId, role, doctorId thành thuộc tính rồi được mapper đưa vào token
public record KeycloakUser(
    String username,
    String password,
    String fullName,
    String email,
    Long accountId,
    String role,
    Long doctorId) {
}
