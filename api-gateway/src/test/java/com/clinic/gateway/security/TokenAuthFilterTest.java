package com.clinic.gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TokenAuthFilterTest {

    private final AtomicReference<ServerWebExchange> reached = new AtomicReference<>();
    private final WebFilterChain chain = exchange -> {
        reached.set(exchange);
        return Mono.empty();
    };

    // Bộ giải mã giả: token "tk-5" là bệnh nhân (tài khoản 5), "tk-2" là lễ tân, "tk-bsan" là bác sĩ 1, "tk-admin" là quản trị
    private final ReactiveJwtDecoder decoder = token -> switch (token) {
        case "tk-5" -> Mono.just(jwt(5, "PATIENT", null));
        case "tk-2" -> Mono.just(jwt(2, "RECEPTIONIST", null));
        case "tk-bsan" -> Mono.just(jwt(3, "DOCTOR", 1));
        case "tk-admin" -> Mono.just(jwt(1, "ADMIN", null));
        case "tk-thieu-claim" -> Mono.just(jwtFrom(Map.of("sub", "abc")));
        default -> Mono.error(new BadJwtException("token sai"));
    };

    private final TokenAuthFilter filter = new TokenAuthFilter(decoder);

    private static Jwt jwt(long accountId, String role, Integer doctorId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("accountId", accountId);
        claims.put("role", role);
        if (doctorId != null) {
            claims.put("doctorId", doctorId);
        }
        return jwtFrom(claims);
    }

    private static Jwt jwtFrom(Map<String, Object> claims) {
        return new Jwt("x", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "RS256"), claims);
    }

    private MockServerWebExchange call(HttpMethod method, String uri, String token, String... extraHeaders) {
        MockServerHttpRequest.BaseBuilder<?> builder = MockServerHttpRequest.method(method, uri);
        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        for (int i = 0; i < extraHeaders.length; i += 2) {
            builder.header(extraHeaders[i], extraHeaders[i + 1]);
        }
        MockServerWebExchange exchange = MockServerWebExchange.from(builder);
        reached.set(null);
        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        return exchange;
    }

    private void expectError(HttpMethod method, String uri, String token, HttpStatus status, String... headers) {
        MockServerHttpRequest.BaseBuilder<?> builder = MockServerHttpRequest.method(method, uri);
        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        for (int i = 0; i < headers.length; i += 2) {
            builder.header(headers[i], headers[i + 1]);
        }
        reached.set(null);
        StepVerifier.create(filter.filter(MockServerWebExchange.from(builder), chain))
            .expectErrorSatisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(status.value()))
            .verify();
        assertThat(reached.get()).as("request không được tới service").isNull();
    }

    @Test
    void khongToken_duongDanKin_401() {
        expectError(HttpMethod.GET, "/api/v1/patients", null, HttpStatus.UNAUTHORIZED);
    }

    @Test
    void tokenSai_401() {
        expectError(HttpMethod.GET, "/api/v1/patients", "tk-gia", HttpStatus.UNAUTHORIZED);
    }

    @Test
    void tokenThieuClaimDanhTinh_401() {
        expectError(HttpMethod.GET, "/api/v1/patients", "tk-thieu-claim", HttpStatus.UNAUTHORIZED);
    }

    @Test
    void tokenHopLe_gatLaiHeaderTuToken() {
        call(HttpMethod.GET, "/api/v1/patients", "tk-2");
        HttpHeaders h = reached.get().getRequest().getHeaders();
        assertThat(h.getFirst("X-User-Id")).isEqualTo("2");
        assertThat(h.getFirst("X-User-Role")).isEqualTo("RECEPTIONIST");
        assertThat(h.getFirst("X-Doctor-Id")).isNull();
    }

    @Test
    void bacSi_coXDoctorId() {
        call(HttpMethod.GET, "/api/v1/slots?doctorId=1&date=2026-11-10", "tk-bsan");
        assertThat(reached.get().getRequest().getHeaders().getFirst("X-Doctor-Id")).isEqualTo("1");
    }

    @Test
    void clientTuGuiXUserId1_vanLaTaiKhoan5() {
        call(HttpMethod.GET, "/api/v1/appointments/mine", "tk-5",
            "X-User-Id", "1", "X-User-Role", "ADMIN", "X-Doctor-Id", "9");
        HttpHeaders h = reached.get().getRequest().getHeaders();
        assertThat(h.get("X-User-Id")).containsExactly("5");
        assertThat(h.get("X-User-Role")).containsExactly("PATIENT");
        assertThat(h.get("X-Doctor-Id")).isNull();
    }

    @Test
    void duongDanCongKhai_khongTokenVanQua_vaXoaHeaderGia() {
        call(HttpMethod.GET, "/api/v1/doctors", null, "X-User-Id", "1");
        assertThat(reached.get()).isNotNull();
        assertThat(reached.get().getRequest().getHeaders().getFirst("X-User-Id")).isNull();
        call(HttpMethod.POST, "/api/v1/auth/register", null);
        assertThat(reached.get()).isNotNull();
        call(HttpMethod.GET, "/actuator/health", null);
        assertThat(reached.get()).isNotNull();
    }

    @Test
    void duongDanCongKhai_tokenSaiVanQuaNhuKhach() {
        call(HttpMethod.GET, "/api/v1/specialties", "tk-gia");
        assertThat(reached.get()).isNotNull();
        assertThat(reached.get().getRequest().getHeaders().getFirst("X-User-Id")).isNull();
    }

    @Test
    void ghiVaoDanhMucBangGETcongKhaiNhungPOSTthiPhaiDangNhap() {
        expectError(HttpMethod.POST, "/api/v1/doctors", null, HttpStatus.UNAUTHORIZED);
    }

    @Test
    void benhNhanGoiPostDoctors_403() {
        expectError(HttpMethod.POST, "/api/v1/doctors", "tk-5", HttpStatus.FORBIDDEN);
    }

    @Test
    void quanTriGoiPostDoctors_qua() {
        call(HttpMethod.POST, "/api/v1/doctors", "tk-admin");
        assertThat(reached.get()).isNotNull();
    }

    @Test
    void preflightOptions_khongCanToken() {
        call(HttpMethod.OPTIONS, "/api/v1/patients", null);
        assertThat(reached.get()).isNotNull();
    }

    @Test
    void doctorAgenda_bacSiChiXemCuaMinh() {
        call(HttpMethod.GET, "/api/v1/composite/doctor-agenda?doctorId=1&date=2026-11-10", "tk-bsan");
        assertThat(reached.get()).isNotNull();
        expectError(HttpMethod.GET, "/api/v1/composite/doctor-agenda?doctorId=2&date=2026-11-10", "tk-bsan",
            HttpStatus.FORBIDDEN);
        expectError(HttpMethod.GET, "/api/v1/composite/doctor-agenda?date=2026-11-10", "tk-bsan", HttpStatus.FORBIDDEN);
    }

    @Test
    void doctorAgenda_leTanXemMoiBacSi_benhNhanBiChan() {
        call(HttpMethod.GET, "/api/v1/composite/doctor-agenda?doctorId=2&date=2026-11-10", "tk-2");
        assertThat(reached.get()).isNotNull();
        expectError(HttpMethod.GET, "/api/v1/composite/doctor-agenda?doctorId=1&date=2026-11-10", "tk-5",
            HttpStatus.FORBIDDEN);
    }

    @Test
    void danhTinhDuocChuyenTiepChoWebClientNoiBo() {
        var captured = new AtomicReference<org.springframework.web.reactive.function.client.ClientRequest>();
        WebFilterChain inner = exchange -> TokenAuthFilter.forwardToDownstream()
            .filter(org.springframework.web.reactive.function.client.ClientRequest
                    .create(HttpMethod.GET, java.net.URI.create("http://patient-service/api/v1/patients/mine")).build(),
                req -> {
                    captured.set(req);
                    return Mono.empty();
                }).then();
        StepVerifier.create(filter.filter(MockServerWebExchange.from(MockServerHttpRequest
            .get("/api/v1/composite/my-appointments").header(HttpHeaders.AUTHORIZATION, "Bearer tk-5")), inner))
            .verifyComplete();
        assertThat(captured.get().headers().getFirst("X-User-Id")).isEqualTo("5");
        assertThat(captured.get().headers().getFirst("X-User-Role")).isEqualTo("PATIENT");
    }
}
