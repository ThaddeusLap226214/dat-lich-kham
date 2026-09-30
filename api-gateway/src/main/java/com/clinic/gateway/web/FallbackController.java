package com.clinic.gateway.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class FallbackController {

    @RequestMapping("/fallback")
    public ResponseEntity<Map<String, Object>> fallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
            "timestamp", LocalDateTime.now().toString(),
            "status", 503,
            "error", "SERVICE_UNAVAILABLE",
            "message", "Dịch vụ tạm thời không phản hồi, vui lòng thử lại sau"));
    }
}
