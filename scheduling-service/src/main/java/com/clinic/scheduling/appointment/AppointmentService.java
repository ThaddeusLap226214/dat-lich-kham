package com.clinic.scheduling.appointment;

import com.clinic.scheduling.appointment.dto.AppointmentResponse;
import com.clinic.scheduling.appointment.dto.HoldRequest;
import com.clinic.scheduling.client.ProviderClient;
import com.clinic.scheduling.common.NotImplementedYetException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ProviderClient providerClient;
    private final int holdMinutes;
    private final int cancelBeforeHours;

    public AppointmentService(AppointmentRepository appointmentRepository, ProviderClient providerClient,
                              @Value("${scheduling.hold-minutes}") int holdMinutes,
                              @Value("${scheduling.cancel-before-hours}") int cancelBeforeHours) {
        this.appointmentRepository = appointmentRepository;
        this.providerClient = providerClient;
        this.holdMinutes = holdMinutes;
        this.cancelBeforeHours = cancelBeforeHours;
    }

    public AppointmentResponse hold(HoldRequest request, Long accountId) {
        throw new NotImplementedYetException(
            "kiểm tra startTime khớp một slot trong ca của bác sĩ (400 SLOT_NOT_IN_SCHEDULE), bệnh nhân không có lịch chồng giờ "
                + "(409 PATIENT_TIME_CONFLICT), lưu HELD; lỗi trùng uk_appointments_slot_lock → 409 SLOT_ALREADY_HELD");
    }

    public AppointmentResponse confirm(Long id, Long accountId) {
        throw new NotImplementedYetException("HELD → CONFIRMED; quá holdExpiresAt → 409 HOLD_EXPIRED; chỉ người giữ chỗ được xác nhận");
    }

    public AppointmentResponse cancel(Long id, String reason, Long accountId, String role) {
        throw new NotImplementedYetException("CONFIRMED → CANCELLED; PATIENT phải hủy trước giờ khám cancelBeforeHours giờ (409 TOO_LATE_TO_CANCEL)");
    }

    public AppointmentResponse complete(Long id, Long doctorId, String role) {
        throw new NotImplementedYetException("CONFIRMED → COMPLETED; DOCTOR chỉ đánh dấu lịch của chính mình");
    }

    public AppointmentResponse get(Long id) {
        throw new NotImplementedYetException("chi tiết lịch hẹn");
    }

    public List<AppointmentResponse> listMine(Long accountId) {
        throw new NotImplementedYetException("lịch hẹn do tài khoản này đặt");
    }

    public List<AppointmentResponse> listByDoctor(Long doctorId, LocalDate date, AppointmentStatus status) {
        throw new NotImplementedYetException("lịch hẹn của bác sĩ trong ngày, lọc theo trạng thái nếu có");
    }

    public List<AppointmentResponse> search(Long bookedByAccountId, Long doctorId, AppointmentStatus status,
                                            LocalDateTime from, LocalDateTime to) {
        throw new NotImplementedYetException("tìm lịch hẹn cho API nội bộ (Gateway ghép dữ liệu, Reminder lấy lịch sắp tới)");
    }

    public int expireHolds() {
        throw new NotImplementedYetException("gọi appointmentRepository.expireHolds(now) trong một transaction");
    }
}
