package com.clinic.gateway.web;

import java.time.LocalDateTime;

// Cùng dạng lỗi với các service (docs/06 A4): lỗi do chính Gateway trả cũng phải theo dạng này (R19)
public record ApiError(LocalDateTime timestamp, int status, String error, String message, String path) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(LocalDateTime.now(), status, error, message, path);
    }
}
