package com.clinic.gateway.security;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

// Chặn thô theo vai trò ở những API đánh dấu (GW) trong docs/03-hop-dong-api.md. Quy tắc đầu tiên khớp được dùng;
// đường dẫn không khớp quy tắc nào thì chỉ cần đã đăng nhập, phần còn lại do từng service tự kiểm
public final class RoleRules {

    private record Rule(Predicate<HttpMethod> method, PathPattern pattern, Set<String> roles) {
    }

    private static final PathPatternParser PARSER = new PathPatternParser();
    private static final Predicate<HttpMethod> ANY = m -> true;
    private static final Predicate<HttpMethod> READ = HttpMethod.GET::equals;
    private static final Predicate<HttpMethod> WRITE = READ.negate();

    private static final Set<String> ADMIN = Set.of("ADMIN");
    private static final Set<String> RECEPTIONIST = Set.of("RECEPTIONIST");
    private static final Set<String> ADMIN_RECEPTIONIST = Set.of("ADMIN", "RECEPTIONIST");
    private static final Set<String> RECEPTIONIST_DOCTOR = Set.of("RECEPTIONIST", "DOCTOR");
    private static final Set<String> PATIENT_RECEPTIONIST = Set.of("PATIENT", "RECEPTIONIST");
    private static final Set<String> EVERYONE = Set.of("PATIENT", "RECEPTIONIST", "DOCTOR", "ADMIN");

    private static final List<Rule> RULES = List.of(
        rule(READ, "/api/v1/accounts/me", EVERYONE),
        rule(ANY, "/api/v1/accounts/**", ADMIN),

        rule(WRITE, "/api/v1/specialties/**", ADMIN),
        rule(READ, "/api/v1/rooms/**", ADMIN_RECEPTIONIST),
        rule(WRITE, "/api/v1/rooms/**", ADMIN),
        rule(WRITE, "/api/v1/doctors/**", ADMIN),
        rule(WRITE, "/api/v1/work-schedules/**", ADMIN),

        rule(READ, "/api/v1/patients", RECEPTIONIST_DOCTOR),
        rule(READ, "/api/v1/patients/*/merge-history", RECEPTIONIST),
        rule(WRITE, "/api/v1/patients/*/identity", RECEPTIONIST),
        rule(WRITE, "/api/v1/patients/*/unmerge", RECEPTIONIST),
        rule(ANY, "/api/v1/duplicate-candidates/**", RECEPTIONIST),

        rule(WRITE, "/api/v1/appointments/hold", PATIENT_RECEPTIONIST),
        rule(READ, "/api/v1/appointments", RECEPTIONIST_DOCTOR),

        rule(READ, "/api/v1/reminders/logs", ADMIN_RECEPTIONIST),
        rule(ANY, "/api/v1/reminders/**", ADMIN),

        rule(READ, "/api/v1/composite/doctor-agenda", RECEPTIONIST_DOCTOR)
    );

    private RoleRules() {
    }

    private static Rule rule(Predicate<HttpMethod> method, String pattern, Set<String> roles) {
        return new Rule(method, PARSER.parse(pattern), roles);
    }

    public static boolean isAllowed(HttpMethod method, String path, String role) {
        PathContainer container = PathContainer.parsePath(path);
        Optional<Rule> found = RULES.stream()
            .filter(r -> r.method().test(method) && r.pattern().matches(container))
            .findFirst();
        return found.map(r -> role != null && r.roles().contains(role)).orElse(true);
    }

    // doctor-agenda: bác sĩ chỉ xem lịch của chính mình (F37)
    public static boolean isDoctorAgendaAllowed(Identity identity, String doctorIdParam) {
        if (!"DOCTOR".equals(identity.role())) {
            return true;
        }
        return identity.doctorId() != null && String.valueOf(identity.doctorId()).equals(doctorIdParam);
    }
}
