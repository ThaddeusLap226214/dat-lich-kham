package com.clinic.auth.keycloak;

import com.clinic.auth.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

// Gọi Keycloak Admin API bằng client auth-service (client credentials). Chỉ Auth Service gọi API này (R23)
@Component
public class KeycloakAdminClient {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminClient.class);
    private static final ParameterizedTypeReference<Map<String, Object>> JSON_OBJECT = new ParameterizedTypeReference<>() {
    };

    private final RestClient restClient;
    private final String realm;
    private final String clientId;
    private final String clientSecret;

    private String cachedToken;
    private Instant cachedTokenExpiresAt = Instant.EPOCH;

    public KeycloakAdminClient(@Value("${keycloak.base-url}") String baseUrl,
                               @Value("${keycloak.realm}") String realm,
                               @Value("${keycloak.client-id}") String clientId,
                               @Value("${keycloak.client-secret}") String clientSecret,
                               @Value("${keycloak.timeout}") Duration timeout) {
        JdkClientHttpRequestFactory requestFactory =
            new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1).connectTimeout(timeout).build());
        requestFactory.setReadTimeout(timeout);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    // Trả về mã người dùng (UUID) mà Keycloak ghi ở cuối header Location
    public String createUser(KeycloakUser user) {
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("accountId", List.of(String.valueOf(user.accountId())));
        attributes.put("role", List.of(user.role()));
        if (user.doctorId() != null) {
            attributes.put("doctorId", List.of(String.valueOf(user.doctorId())));
        }
        Map<String, Object> body = new HashMap<>();
        body.put("username", user.username());
        body.put("enabled", true);
        body.put("firstName", user.fullName());
        body.put("email", user.email());
        body.put("emailVerified", false);
        body.put("attributes", attributes);
        body.put("credentials", List.of(Map.of("type", "password", "value", user.password(), "temporary", false)));

        URI location = call(() -> restClient.post()
            .uri("/admin/realms/{realm}/users", realm)
            .headers(h -> h.setBearerAuth(token()))
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .toBodilessEntity()
            .getHeaders()
            .getLocation());
        String path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    public void assignRealmRole(String userId, String roleName) {
        Map<String, Object> role = call(() -> restClient.get()
            .uri("/admin/realms/{realm}/roles/{role}", realm, roleName)
            .headers(h -> h.setBearerAuth(token()))
            .retrieve()
            .body(JSON_OBJECT));
        call(() -> restClient.post()
            .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", realm, userId)
            .headers(h -> h.setBearerAuth(token()))
            .contentType(MediaType.APPLICATION_JSON)
            .body(List.of(role))
            .retrieve()
            .toBodilessEntity());
    }

    // Đọc nguyên bản ghi rồi gửi lại: gửi thiếu trường có thể làm Keycloak xóa thuộc tính accountId, role
    public void setEnabled(String userId, boolean enabled) {
        Map<String, Object> user = call(() -> restClient.get()
            .uri("/admin/realms/{realm}/users/{id}", realm, userId)
            .headers(h -> h.setBearerAuth(token()))
            .retrieve()
            .body(JSON_OBJECT));
        user.put("enabled", enabled);
        call(() -> restClient.put()
            .uri("/admin/realms/{realm}/users/{id}", realm, userId)
            .headers(h -> h.setBearerAuth(token()))
            .contentType(MediaType.APPLICATION_JSON)
            .body(user)
            .retrieve()
            .toBodilessEntity());
    }

    // Dùng khi bù trừ: lỗi ở đây chỉ ghi log để không che mất lỗi gốc
    public void deleteUserQuietly(String userId) {
        try {
            call(() -> restClient.delete()
                .uri("/admin/realms/{realm}/users/{id}", realm, userId)
                .headers(h -> h.setBearerAuth(token()))
                .retrieve()
                .toBodilessEntity());
        } catch (RuntimeException ex) {
            log.error("Bù trừ thất bại: không xóa được người dùng Keycloak {}, cần xóa tay", userId, ex);
        }
    }

    private synchronized String token() {
        if (cachedToken != null && Instant.now().isBefore(cachedTokenExpiresAt)) {
            return cachedToken;
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        Map<String, Object> response = call(() -> restClient.post()
            .uri("/realms/{realm}/protocol/openid-connect/token", realm)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(JSON_OBJECT));
        cachedToken = (String) response.get("access_token");
        long expiresIn = ((Number) response.get("expires_in")).longValue();
        cachedTokenExpiresAt = Instant.now().plusSeconds(Math.max(0, expiresIn - 10));
        return cachedToken;
    }

    private <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (ResourceAccessException ex) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "KEYCLOAK_UNAVAILABLE",
                "Máy chủ đăng nhập (Keycloak) không phản hồi, vui lòng thử lại sau");
        } catch (HttpClientErrorException.Conflict ex) {
            String detail = ex.getResponseBodyAsString();
            if (detail.contains("email")) {
                throw new BusinessException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "Email đã được dùng cho tài khoản khác");
            }
            throw new BusinessException(HttpStatus.CONFLICT, "USERNAME_TAKEN", "Tên đăng nhập đã có người dùng");
        } catch (RestClientResponseException ex) {
            log.error("Keycloak trả lỗi {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "KEYCLOAK_ERROR",
                "Máy chủ đăng nhập (Keycloak) từ chối yêu cầu");
        }
    }
}
