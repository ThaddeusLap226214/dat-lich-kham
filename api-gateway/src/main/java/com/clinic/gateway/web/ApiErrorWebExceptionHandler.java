package com.clinic.gateway.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.util.List;

// Lỗi phát sinh ngay ở Gateway (đường dẫn không có route như /internal/**, sai phương thức, API ghép chưa làm...)
// trả theo dạng ApiError thay vì dạng mặc định của Spring. Order(-2) để chạy trước bộ xử lý lỗi mặc định
@Component
@Order(-2)
public class ApiErrorWebExceptionHandler implements WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiErrorWebExceptionHandler.class);

    private final List<HttpMessageWriter<?>> messageWriters;

    public ApiErrorWebExceptionHandler(ServerCodecConfigurer codecConfigurer) {
        this.messageWriters = codecConfigurer.getWriters();
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String reason = null;
        if (ex instanceof ResponseStatusException rse) {
            HttpStatus resolved = HttpStatus.resolve(rse.getStatusCode().value());
            status = resolved != null ? resolved : status;
            reason = rse.getReason();
        }
        if (status.is5xxServerError() && status != HttpStatus.NOT_IMPLEMENTED && status != HttpStatus.SERVICE_UNAVAILABLE) {
            log.error("Lỗi không lường trước tại {}", exchange.getRequest().getPath(), ex);
        }
        ApiError body = ApiError.of(status.value(), code(status), message(status, reason),
            exchange.getRequest().getPath().value());
        return ServerResponse.status(status)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .flatMap(response -> response.writeTo(exchange, new ServerResponse.Context() {
                @Override
                public List<HttpMessageWriter<?>> messageWriters() {
                    return messageWriters;
                }

                @Override
                public List<ViewResolver> viewResolvers() {
                    return List.of();
                }
            }));
    }

    private static String code(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "BAD_REQUEST";
            case UNAUTHORIZED -> "UNAUTHORIZED";
            case FORBIDDEN -> "FORBIDDEN";
            case NOT_FOUND -> "NOT_FOUND";
            case METHOD_NOT_ALLOWED -> "METHOD_NOT_ALLOWED";
            case NOT_IMPLEMENTED -> "NOT_IMPLEMENTED";
            case SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT -> "SERVICE_UNAVAILABLE";
            default -> "INTERNAL_ERROR";
        };
    }

    private static String message(HttpStatus status, String reason) {
        return switch (status) {
            case NOT_FOUND -> "Không có đường dẫn này";
            case METHOD_NOT_ALLOWED -> "Đường dẫn này không nhận phương thức đã gửi";
            case NOT_IMPLEMENTED -> reason != null ? reason : "Chưa làm";
            case SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT -> "Dịch vụ tạm thời không phản hồi, vui lòng thử lại sau";
            case INTERNAL_SERVER_ERROR -> "Lỗi hệ thống";
            default -> reason != null ? reason : status.getReasonPhrase();
        };
    }
}
