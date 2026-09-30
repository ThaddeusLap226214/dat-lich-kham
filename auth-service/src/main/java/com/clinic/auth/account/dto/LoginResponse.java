package com.clinic.auth.account.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds, AccountResponse account) {
}
