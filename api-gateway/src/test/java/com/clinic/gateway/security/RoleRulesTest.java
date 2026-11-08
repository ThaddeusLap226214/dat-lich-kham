package com.clinic.gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import static org.assertj.core.api.Assertions.assertThat;

class RoleRulesTest {

    private static boolean ok(HttpMethod m, String path, String role) {
        return RoleRules.isAllowed(m, path, role);
    }

    @Test
    void danhMucChiAdminGhi() {
        assertThat(ok(HttpMethod.POST, "/api/v1/doctors", "PATIENT")).isFalse();
        assertThat(ok(HttpMethod.POST, "/api/v1/doctors", "ADMIN")).isTrue();
        assertThat(ok(HttpMethod.PUT, "/api/v1/doctors/1", "RECEPTIONIST")).isFalse();
        assertThat(ok(HttpMethod.POST, "/api/v1/specialties", "DOCTOR")).isFalse();
        assertThat(ok(HttpMethod.POST, "/api/v1/doctors/1/work-schedules", "ADMIN")).isTrue();
        assertThat(ok(HttpMethod.DELETE, "/api/v1/work-schedules/3", "DOCTOR")).isFalse();
        assertThat(ok(HttpMethod.PUT, "/api/v1/rooms/1", "RECEPTIONIST")).isFalse();
    }

    @Test
    void phongKham_adminVaLeTanXem() {
        assertThat(ok(HttpMethod.GET, "/api/v1/rooms", "RECEPTIONIST")).isTrue();
        assertThat(ok(HttpMethod.GET, "/api/v1/rooms", "ADMIN")).isTrue();
        assertThat(ok(HttpMethod.GET, "/api/v1/rooms", "PATIENT")).isFalse();
    }

    @Test
    void taiKhoan_adminTuyetDoi_meThiMoiVaiTro() {
        assertThat(ok(HttpMethod.GET, "/api/v1/accounts", "RECEPTIONIST")).isFalse();
        assertThat(ok(HttpMethod.POST, "/api/v1/accounts/4/lock", "PATIENT")).isFalse();
        assertThat(ok(HttpMethod.POST, "/api/v1/accounts/4/lock", "ADMIN")).isTrue();
        assertThat(ok(HttpMethod.GET, "/api/v1/accounts/me", "PATIENT")).isTrue();
    }

    @Test
    void benhNhan() {
        assertThat(ok(HttpMethod.GET, "/api/v1/patients", "PATIENT")).isFalse();
        assertThat(ok(HttpMethod.GET, "/api/v1/patients", "DOCTOR")).isTrue();
        assertThat(ok(HttpMethod.GET, "/api/v1/patients/mine", "PATIENT")).isTrue();
        assertThat(ok(HttpMethod.POST, "/api/v1/patients", "PATIENT")).isTrue();
        assertThat(ok(HttpMethod.POST, "/api/v1/patients/2/identity", "DOCTOR")).isFalse();
        assertThat(ok(HttpMethod.POST, "/api/v1/patients/2/identity", "RECEPTIONIST")).isTrue();
        assertThat(ok(HttpMethod.GET, "/api/v1/duplicate-candidates/count", "DOCTOR")).isFalse();
        assertThat(ok(HttpMethod.POST, "/api/v1/duplicate-candidates/3/merge", "RECEPTIONIST")).isTrue();
    }

    @Test
    void lichHen() {
        assertThat(ok(HttpMethod.POST, "/api/v1/appointments/hold", "DOCTOR")).isFalse();
        assertThat(ok(HttpMethod.POST, "/api/v1/appointments/hold", "PATIENT")).isTrue();
        assertThat(ok(HttpMethod.GET, "/api/v1/appointments", "PATIENT")).isFalse();
        assertThat(ok(HttpMethod.GET, "/api/v1/appointments/mine", "PATIENT")).isTrue();
    }

    @Test
    void nhacLich() {
        assertThat(ok(HttpMethod.GET, "/api/v1/reminders/logs", "RECEPTIONIST")).isTrue();
        assertThat(ok(HttpMethod.POST, "/api/v1/reminders/run", "RECEPTIONIST")).isFalse();
        assertThat(ok(HttpMethod.PUT, "/api/v1/reminders/templates/BEFORE_APPOINTMENT", "ADMIN")).isTrue();
        assertThat(ok(HttpMethod.GET, "/api/v1/reminders/templates", "RECEPTIONIST")).isFalse();
    }

    @Test
    void duongDanKhongCoQuyTac_chiCanDangNhap() {
        assertThat(ok(HttpMethod.GET, "/api/v1/slots", "PATIENT")).isTrue();
        assertThat(ok(HttpMethod.GET, "/api/v1/composite/my-appointments", "PATIENT")).isTrue();
    }
}
