package com.clinic.patient.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

// Lỗi nghiệp vụ có mã riêng, ví dụ new BusinessException(HttpStatus.CONFLICT, "SLOT_ALREADY_HELD", "Khung giờ đã có người giữ")
@Getter
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public BusinessException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static BusinessException notFound(String what, Object id) {
        return new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND", what + " " + id + " không tồn tại");
    }
}
