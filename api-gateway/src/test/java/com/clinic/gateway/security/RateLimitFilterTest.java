package com.clinic.gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private final AtomicLong now = new AtomicLong(0);
    private final WebFilterChain chain = exchange -> Mono.empty();

    // Thùng 3 lượt, hồi 1 lượt mỗi giây
    private final RateLimitFilter filter = new RateLimitFilter(3, 1, now::get);

    private static MockServerWebExchange request(HttpMethod method, String path, String accountId, String ip) {
        MockServerHttpRequest.BaseBuilder<?> builder = MockServerHttpRequest.method(method, path)
            .remoteAddress(new InetSocketAddress(ip, 50000));
        if (accountId != null) {
            builder.header(TokenAuthFilter.USER_ID, accountId);
        }
        return MockServerWebExchange.from(builder);
    }

    private static MockServerWebExchange account(String accountId) {
        return request(HttpMethod.GET, "/api/v1/doctors", accountId, "10.0.0.1");
    }

    private void passes(MockServerWebExchange exchange) {
        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
    }

    private void rejected(MockServerWebExchange exchange) {
        StepVerifier.create(filter.filter(exchange, chain))
            .expectErrorSatisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS))
            .verify();
    }

    private void advanceSeconds(double seconds) {
        now.addAndGet((long) (seconds * TimeUnit.SECONDS.toNanos(1)));
    }

    @Test
    void vuotNguongThi429KemRetryAfter() {
        passes(account("5"));
        passes(account("5"));
        MockServerWebExchange third = account("5");
        passes(third);
        assertThat(third.getResponse().getHeaders().getFirst(RateLimitFilter.REMAINING_HEADER)).isEqualTo("0");

        MockServerWebExchange fourth = account("5");
        rejected(fourth);
        assertThat(fourth.getResponse().getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("1");
        assertThat(fourth.getResponse().getHeaders().getFirst(RateLimitFilter.LIMIT_HEADER)).isEqualTo("3");
    }

    @Test
    void choDuThoiGianThiDuocGoiTiep() {
        passes(account("5"));
        passes(account("5"));
        passes(account("5"));
        rejected(account("5"));
        advanceSeconds(1);
        passes(account("5"));
        rejected(account("5"));
    }

    @Test
    void moiTaiKhoanMotThungRieng() {
        passes(account("5"));
        passes(account("5"));
        passes(account("5"));
        rejected(account("5"));
        passes(account("2"));
    }

    @Test
    void cungIpNhungKhacTaiKhoanKhongAnhHuongNhau() {
        for (int i = 0; i < 3; i++) {
            passes(request(HttpMethod.GET, "/api/v1/slots", null, "10.0.0.9"));
        }
        rejected(request(HttpMethod.GET, "/api/v1/slots", null, "10.0.0.9"));
        passes(request(HttpMethod.GET, "/api/v1/slots", null, "10.0.0.8"));
        passes(request(HttpMethod.GET, "/api/v1/slots", "5", "10.0.0.9"));
    }

    @Test
    void khongTinhHealthCheckVaPreflight() {
        for (int i = 0; i < 10; i++) {
            passes(request(HttpMethod.GET, "/actuator/health", null, "10.0.0.1"));
            passes(request(HttpMethod.OPTIONS, "/api/v1/doctors", "5", "10.0.0.1"));
        }
        passes(account("5"));
    }

    @Test
    void capacityBangKhongThiTatGioiHan() {
        RateLimitFilter off = new RateLimitFilter(0, 1, now::get);
        for (int i = 0; i < 50; i++) {
            StepVerifier.create(off.filter(account("5"), chain)).verifyComplete();
        }
    }

    @Test
    void thungLauKhongDungDuocDonDi() {
        passes(account("5"));
        passes(account("2"));
        assertThat(filter.trackedKeys()).isEqualTo(2);
        advanceSeconds(6 * 60);
        passes(account("7"));
        assertThat(filter.trackedKeys()).isEqualTo(1);
    }
}
