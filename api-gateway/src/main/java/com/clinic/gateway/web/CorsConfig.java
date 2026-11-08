package com.clinic.gateway.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

// Bộ lọc CORS chạy trước mọi thứ, nên áp cho cả route chuyển tiếp lẫn API ghép dữ liệu viết trong Gateway
// (cấu hình globalcors của Spring Cloud Gateway chỉ áp cho route chuyển tiếp)
@Configuration
public class CorsConfig {

    // Sau CorrelationIdFilter, trước TokenAuthFilter: lỗi 401 / 403 của bộ lọc token vẫn kèm header CORS
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    public CorsWebFilter corsWebFilter(@Value("${frontend.origin}") String frontendOrigin) {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin(frontendOrigin);
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
