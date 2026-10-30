package com.clinic.gateway.web;

import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import java.net.URI;
import java.util.Set;

@RestController
public class FallbackController {

    // Circuit Breaker chuyển request sang đây (forward:/fallback); đường dẫn gốc client gọi nằm trong thuộc tính của exchange
    @RequestMapping("/fallback")
    public ResponseEntity<ApiError> fallback(ServerWebExchange exchange) {
        Set<URI> originalUrls = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ORIGINAL_REQUEST_URL_ATTR);
        String path = originalUrls == null || originalUrls.isEmpty()
            ? exchange.getRequest().getPath().value()
            : originalUrls.iterator().next().getPath();
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiError.of(503, "SERVICE_UNAVAILABLE",
            "Dịch vụ tạm thời không phản hồi, vui lòng thử lại sau", path));
    }
}
