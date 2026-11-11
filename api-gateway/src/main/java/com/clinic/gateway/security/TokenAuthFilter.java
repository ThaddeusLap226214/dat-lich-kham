package com.clinic.gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Optional;

// P4-4, P4-5: xác minh token Keycloak, xóa X-User-* / X-Doctor-Id client tự gửi rồi gắn lại từ token, chặn theo vai trò.
// Dùng WebFilter (không phải GlobalFilter) để cũng bảo vệ API ghép /api/v1/composite/** do chính Gateway xử lý
@Component
public class TokenAuthFilter implements WebFilter, Ordered {

    public static final String USER_ID = "X-User-Id";
    public static final String USER_ROLE = "X-User-Role";
    public static final String DOCTOR_ID = "X-Doctor-Id";
    private static final String CONTEXT_KEY = "identity";
    private static final String BEARER = "Bearer ";
    private static final Logger log = LoggerFactory.getLogger(TokenAuthFilter.class);

    private final ReactiveJwtDecoder decoder;

    public TokenAuthFilter(ReactiveJwtDecoder decoder) {
        this.decoder = decoder;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerWebExchange clean = exchange.mutate().request(request.mutate().headers(h -> {
            h.remove(USER_ID);
            h.remove(USER_ROLE);
            h.remove(DOCTOR_ID);
        }).build()).build();

        HttpMethod method = request.getMethod();
        if (HttpMethod.OPTIONS.equals(method)) {
            return chain.filter(clean);
        }
        String path = request.getPath().pathWithinApplication().value();
        boolean isPublic = PublicPaths.matches(method, path);

        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
            return isPublic ? chain.filter(clean) : Mono.error(unauthorized("Cần đăng nhập"));
        }
        String token = authorization.substring(BEARER.length()).trim();

        Mono<Optional<Jwt>> verified = decoder.decode(token).map(Optional::of)
            .onErrorResume(JwtException.class, e -> {
                log.debug("Token bị từ chối: {}", e.getMessage());
                return Mono.just(Optional.empty());
            });
        return verified.flatMap(jwt -> {
            Optional<Identity> identity = jwt.flatMap(TokenAuthFilter::identityOf);
            if (identity.isEmpty()) {
                return isPublic ? chain.filter(clean) : Mono.error(unauthorized("Token không hợp lệ hoặc đã hết hạn"));
            }
            return proceed(clean, chain, identity.get(), method, path, isPublic);
        });
    }

    private Mono<Void> proceed(ServerWebExchange clean, WebFilterChain chain, Identity identity, HttpMethod method,
                               String path, boolean isPublic) {
        if (!isPublic) {
            if (!RoleRules.isAllowed(method, path, identity.role())) {
                return Mono.error(forbidden());
            }
            if ("/api/v1/composite/doctor-agenda".equals(path)
                && !RoleRules.isDoctorAgendaAllowed(identity, clean.getRequest().getQueryParams().getFirst("doctorId"))) {
                return Mono.error(forbidden());
            }
        }
        ServerWebExchange withIdentity = clean.mutate().request(clean.getRequest().mutate().headers(h -> {
            h.set(USER_ID, String.valueOf(identity.accountId()));
            h.set(USER_ROLE, identity.role());
            if (identity.doctorId() != null) {
                h.set(DOCTOR_ID, String.valueOf(identity.doctorId()));
            }
        }).build()).build();
        return chain.filter(withIdentity).contextWrite(ctx -> ctx.put(CONTEXT_KEY, identity));
    }

    static Optional<Identity> identityOf(Jwt jwt) {
        Long accountId = asLong(jwt.getClaim("accountId"));
        String role = jwt.getClaimAsString("role");
        if (accountId == null || role == null || role.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new Identity(accountId, role, asLong(jwt.getClaim("doctorId"))));
    }

    private static Long asLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value instanceof String s && !s.isBlank()) {
            try {
                return Long.valueOf(s.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static ResponseStatusException unauthorized(String reason) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, reason);
    }

    private static ResponseStatusException forbidden() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Vai trò của bạn không được dùng chức năng này");
    }

    // Sau CORS (để lỗi 401 / 403 vẫn kèm header CORS), trước các bộ lọc còn lại
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }

    // WebClient của API ghép gửi tiếp danh tính của request đang xử lý (vd. Patient /mine cần X-User-Id)
    public static ExchangeFilterFunction forwardToDownstream() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> Mono.deferContextual(ctx -> {
            if (!ctx.hasKey(CONTEXT_KEY)) {
                return Mono.just(request);
            }
            Identity identity = ctx.get(CONTEXT_KEY);
            ClientRequest.Builder builder = ClientRequest.from(request)
                .header(USER_ID, String.valueOf(identity.accountId()))
                .header(USER_ROLE, identity.role());
            if (identity.doctorId() != null) {
                builder.header(DOCTOR_ID, String.valueOf(identity.doctorId()));
            }
            return Mono.just(builder.build());
        }));
    }
}
