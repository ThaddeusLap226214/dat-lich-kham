package com.clinic.gateway.security;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.List;

// Đường dẫn không cần đăng nhập (docs/03-hop-dong-api.md, mục A5 khoản 5) và kiểm tra sức khỏe cho Docker
public final class PublicPaths {

    private record Entry(HttpMethod method, PathPattern pattern) {
    }

    private static final PathPatternParser PARSER = new PathPatternParser();

    private static final List<Entry> ENTRIES = List.of(
        entry(HttpMethod.POST, "/api/v1/auth/register"),
        entry(HttpMethod.GET, "/api/v1/specialties/**"),
        entry(HttpMethod.GET, "/api/v1/doctors/**"),
        entry(HttpMethod.GET, "/actuator/health/**")
    );

    private PublicPaths() {
    }

    private static Entry entry(HttpMethod method, String pattern) {
        return new Entry(method, PARSER.parse(pattern));
    }

    public static boolean matches(HttpMethod method, String path) {
        PathContainer container = PathContainer.parsePath(path);
        return ENTRIES.stream().anyMatch(e -> e.method().equals(method) && e.pattern().matches(container));
    }
}
