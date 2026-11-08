package com.clinic.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

// Chữ ký lấy từ khóa công khai (JWKS) của Keycloak, tải khi cần rồi giữ trong bộ nhớ; kiểm hạn dùng và iss.
// iss là địa chỉ trình duyệt thấy (localhost:8180), còn JWKS lấy qua địa chỉ trong mạng Docker (keycloak:8080)
@Configuration
public class JwtDecoderConfig {

    @Bean
    public ReactiveJwtDecoder jwtDecoder(@Value("${auth.issuer}") String issuer,
                                         @Value("${auth.jwk-set-uri}") String jwkSetUri) {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }
}
