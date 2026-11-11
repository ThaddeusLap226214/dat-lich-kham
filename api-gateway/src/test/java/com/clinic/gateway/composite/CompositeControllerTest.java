package com.clinic.gateway.composite;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "rawtypes"})
class CompositeControllerTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 10, 5, 8, 0);

    private SchedulingClient scheduling;
    private ProviderClient provider;
    private PatientClient patient;
    private CompositeController controller;

    @BeforeEach
    void setUp() {
        scheduling = mock(SchedulingClient.class);
        provider = mock(ProviderClient.class);
        patient = mock(PatientClient.class);

        // Mạch ngắt giả: chạy lời gọi, lỗi thì chuyển sang fallback, giống hành vi thật của Resilience4j
        ReactiveCircuitBreaker passThrough = new ReactiveCircuitBreaker() {
            @Override
            public <T> Mono<T> run(Mono<T> toRun, Function<Throwable, Mono<T>> fallback) {
                return toRun.onErrorResume(fallback);
            }

            @Override
            public <T> Flux<T> run(Flux<T> toRun, Function<Throwable, Flux<T>> fallback) {
                return toRun.onErrorResume(fallback);
            }
        };
        ReactiveCircuitBreakerFactory factory = mock(ReactiveCircuitBreakerFactory.class);
        when(factory.create(anyString())).thenReturn(passThrough);

        controller = new CompositeController(scheduling, provider, patient, factory);
    }

    private static RemoteData.Appointment appointment(long id, long patientId, String reason, String cancelReason) {
        return appointment(id, patientId, 5L, reason, cancelReason);
    }

    private static RemoteData.Appointment appointment(long id, long patientId, long bookedBy, String reason, String cancelReason) {
        return new RemoteData.Appointment(id, patientId, 1L, 1L, bookedBy, START.plusHours(id), START.plusHours(id).plusMinutes(20),
            cancelReason == null ? "CONFIRMED" : "CANCELLED", reason, cancelReason);
    }

    private void stubProviderAndPatientOk() {
        when(provider.findDoctors(anyCollection())).thenReturn(
            Flux.just(new RemoteData.Doctor(1L, "Nguyễn Văn An", "ThS.BS", "Nội tổng quát")));
        when(provider.findRooms(anyCollection())).thenReturn(
            Flux.just(new RemoteData.Room(1L, "P101", "Phòng khám Nội 1")));
        when(patient.findPatients(anyCollection())).thenReturn(Flux.just(
            new RemoteData.Patient(1L, "Lê Thị Hoa", LocalDate.of(1990, 5, 12)),
            new RemoteData.Patient(2L, "Lê Văn Bình", LocalDate.of(2018, 1, 3))));
    }

    @Test
    void duDuLieu_ghepDuTenBacSiPhongBenhNhan_gomCaLichCuaHoSoMinhKhongDoMinhDat() {
        when(patient.findMine(5L)).thenReturn(Flux.just(new RemoteData.Patient(1L, "Lê Thị Hoa", null),
            new RemoteData.Patient(2L, "Lê Văn Bình", null)));
        // Lịch 12 do tài khoản đặt; lịch 13 thuộc hồ sơ 2 nhưng do người khác (lễ tân) đặt, Scheduling trả cả hai
        when(scheduling.findByAccount(eq(5L), eq(List.of(1L, 2L)))).thenReturn(Flux.just(
            appointment(12, 1, "Ho kéo dài", null), appointment(13, 2, 2L, "Khám định kỳ", "Bận việc")));
        stubProviderAndPatientOk();

        StepVerifier.create(controller.myAppointments(5L)).assertNext(views -> {
            assertThat(views).hasSize(2);
            AppointmentView first = views.get(0);
            assertThat(first.appointmentId()).isEqualTo(12L);
            assertThat(first.status()).isEqualTo("CONFIRMED");
            assertThat(first.reason()).isEqualTo("Ho kéo dài");
            assertThat(first.cancelReason()).isNull();
            assertThat(first.bookedByAccountId()).isEqualTo(5L);
            assertThat(first.doctor()).isEqualTo(new AppointmentView.DoctorView(1L, "Nguyễn Văn An", "ThS.BS", "Nội tổng quát"));
            assertThat(first.room()).isEqualTo(new AppointmentView.RoomView(1L, "P101", "Phòng khám Nội 1"));
            assertThat(first.patient()).isEqualTo(new AppointmentView.PatientView(1L, "Lê Thị Hoa", "1990-05-12"));
            AppointmentView second = views.get(1);
            assertThat(second.patient().fullName()).isEqualTo("Lê Văn Bình");
            assertThat(second.status()).isEqualTo("CANCELLED");
            assertThat(second.cancelReason()).isEqualTo("Bận việc");
            assertThat(second.bookedByAccountId()).isEqualTo(2L);
        }).verifyComplete();
    }

    @Test
    void providerLoi_vanTraDanhSach_doctorVaRoomLaNull() {
        when(patient.findMine(5L)).thenReturn(Flux.empty());
        when(scheduling.findByAccount(eq(5L), anyCollection())).thenReturn(Flux.just(appointment(12, 1, "Ho", null)));
        when(provider.findDoctors(anyCollection())).thenReturn(Flux.error(new IllegalStateException("provider down")));
        when(provider.findRooms(anyCollection())).thenReturn(Flux.error(new IllegalStateException("provider down")));
        when(patient.findPatients(anyCollection())).thenReturn(
            Flux.just(new RemoteData.Patient(1L, "Lê Thị Hoa", LocalDate.of(1990, 5, 12))));

        StepVerifier.create(controller.myAppointments(5L)).assertNext(views -> {
            assertThat(views).hasSize(1);
            assertThat(views.get(0).doctor()).isNull();
            assertThat(views.get(0).room()).isNull();
            assertThat(views.get(0).patient().fullName()).isEqualTo("Lê Thị Hoa");
        }).verifyComplete();
    }

    @Test
    void patientLoiOBuoc1_vanTraLichDoTaiKhoanDat_vaKhongGuiPatientIds() {
        when(patient.findMine(5L)).thenReturn(Flux.error(new IllegalStateException("501")));
        when(scheduling.findByAccount(eq(5L), eq(List.<Long>of()))).thenReturn(Flux.just(appointment(12, 1, "Ho", null)));
        when(provider.findDoctors(anyCollection())).thenReturn(
            Flux.just(new RemoteData.Doctor(1L, "Nguyễn Văn An", "ThS.BS", "Nội tổng quát")));
        when(provider.findRooms(anyCollection())).thenReturn(Flux.just(new RemoteData.Room(1L, "P101", "Phòng khám Nội 1")));
        when(patient.findPatients(anyCollection())).thenReturn(Flux.error(new IllegalStateException("501")));

        StepVerifier.create(controller.myAppointments(5L)).assertNext(views -> {
            assertThat(views).hasSize(1);
            assertThat(views.get(0).doctor().fullName()).isEqualTo("Nguyễn Văn An");
            assertThat(views.get(0).patient()).isNull();
        }).verifyComplete();
        verify(scheduling).findByAccount(5L, List.of());
    }

    @Test
    void schedulingLoi_tra503() {
        when(patient.findMine(5L)).thenReturn(Flux.empty());
        when(scheduling.findByAccount(eq(5L), anyCollection())).thenReturn(Flux.error(new IllegalStateException("501")));

        StepVerifier.create(controller.myAppointments(5L)).expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(ResponseStatusException.class);
            assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        }).verify();
        verify(provider, never()).findDoctors(any());
    }

    @Test
    void doctorAgenda_ghepNhuMyAppointments_vaSchedulingLoiThi503() {
        LocalDate date = LocalDate.of(2026, 10, 5);
        when(scheduling.findByDoctorAndDate(eq(1L), eq(date))).thenReturn(Flux.just(appointment(12, 1, "Ho", null)));
        stubProviderAndPatientOk();

        StepVerifier.create(controller.doctorAgenda(1L, date)).assertNext(views -> {
            assertThat(views).hasSize(1);
            assertThat(views.get(0).patient().fullName()).isEqualTo("Lê Thị Hoa");
            assertThat(views.get(0).doctor().fullName()).isEqualTo("Nguyễn Văn An");
        }).verifyComplete();

        when(scheduling.findByDoctorAndDate(anyLong(), any())).thenReturn(Flux.error(new IllegalStateException("down")));
        StepVerifier.create(controller.doctorAgenda(1L, date)).expectErrorSatisfies(error ->
            assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)).verify();
    }
}
