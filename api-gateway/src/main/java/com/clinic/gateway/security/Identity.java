package com.clinic.gateway.security;

// Danh tính lấy từ claim accountId, role, doctorId của token (docs/03-hop-dong-api.md, mục A5)
public record Identity(Long accountId, String role, Long doctorId) {
}
