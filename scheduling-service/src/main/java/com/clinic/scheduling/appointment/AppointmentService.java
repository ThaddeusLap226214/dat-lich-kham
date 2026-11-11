package com.clinic.scheduling.appointment;

import com.clinic.scheduling.appointment.dto.AppointmentResponse;
import com.clinic.scheduling.appointment.dto.HoldRequest;
import com.clinic.scheduling.client.ProviderClient;
import com.clinic.scheduling.client.WorkingHours;
import com.clinic.scheduling.common.BusinessException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ProviderClient providerClient;
    private final com.clinic.scheduling.event.AppointmentEventPublisher eventPublisher;
    private final int holdMinutes;
    private final int cancelBeforeHours;
    private final int maxHoldsPerAccount;
    private final int maxUpcomingPerPatient;
    private final int bookAheadDays;
    private final int bookBeforeHours;

    public AppointmentService(AppointmentRepository appointmentRepository, ProviderClient providerClient,
                              com.clinic.scheduling.event.AppointmentEventPublisher eventPublisher,
                              @Value("${scheduling.hold-minutes:5}") int holdMinutes,
                              @Value("${scheduling.cancel-before-hours:2}") int cancelBeforeHours,
                              @Value("${scheduling.max-holds-per-account:3}") int maxHoldsPerAccount,
                              @Value("${scheduling.max-upcoming-per-patient:5}") int maxUpcomingPerPatient,
                              @Value("${scheduling.book-ahead-days:30}") int bookAheadDays,
                              @Value("${scheduling.book-before-hours:2}") int bookBeforeHours) {
        this.appointmentRepository = appointmentRepository;
        this.providerClient = providerClient;
        this.eventPublisher = eventPublisher;
        this.holdMinutes = holdMinutes;
        this.cancelBeforeHours = cancelBeforeHours;
        this.maxHoldsPerAccount = maxHoldsPerAccount;
        this.maxUpcomingPerPatient = maxUpcomingPerPatient;
        this.bookAheadDays = bookAheadDays;
        this.bookBeforeHours = bookBeforeHours;
    }

    @Transactional
    public AppointmentResponse hold(HoldRequest request, Long accountId, String role) {
        LocalDateTime now = LocalDateTime.now();
        // Nhả các chỗ quá hạn trước
        appointmentRepository.expireHolds(now);

        LocalDate date = request.startTime().toLocalDate();
        LocalTime time = request.startTime().toLocalTime();

        // 1. Kiểm tra CS05: Đặt trước tối đa bookAheadDays (30 ngày)
        if (date.isAfter(LocalDate.now().plusDays(bookAheadDays))) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TOO_FAR_IN_ADVANCE",
                "Chỉ được đặt lịch trước tối đa " + bookAheadDays + " ngày");
        }

        // 2. Kiểm tra CS06: Bệnh nhân (PATIENT) không được đặt khi còn dưới bookBeforeHours (2 giờ)
        if ("PATIENT".equals(role) && request.startTime().isBefore(now.plusHours(bookBeforeHours))) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TOO_LATE_TO_BOOK",
                "Bệnh nhân phải đặt lịch trước giờ khám ít nhất " + bookBeforeHours + " giờ");
        }

        // 3. Kiểm tra CS03: Số chỗ đang giữ của một tài khoản tối đa maxHoldsPerAccount (3 chỗ)
        int currentHolds = appointmentRepository.countByBookedByAccountIdAndStatus(accountId, AppointmentStatus.HELD);
        if (currentHolds >= maxHoldsPerAccount) {
            throw new BusinessException(HttpStatus.CONFLICT, "TOO_MANY_HOLDS",
                "Bạn chỉ được giữ tối đa " + maxHoldsPerAccount + " chỗ khám cùng lúc");
        }

        // 4. Kiểm tra CS04: Số lịch sắp tới của một hồ sơ tối đa maxUpcomingPerPatient (5 lịch)
        int upcomingCount = appointmentRepository.countByPatientIdAndStatusInAndStartTimeAfter(
            request.patientId(),
            List.of(AppointmentStatus.HELD, AppointmentStatus.CONFIRMED),
            now
        );
        if (upcomingCount >= maxUpcomingPerPatient) {
            throw new BusinessException(HttpStatus.CONFLICT, "TOO_MANY_APPOINTMENTS",
                "Hồ sơ này đã có " + upcomingCount + " lịch hẹn sắp tới, tối đa cho phép là " + maxUpcomingPerPatient);
        }

        // 5. Lấy giờ làm việc của bác sĩ từ Provider Service và khớp slot
        WorkingHours workingHours = providerClient.getWorkingHours(request.doctorId(), date);
        if (workingHours == null || workingHours.shifts() == null || workingHours.shifts().isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "SLOT_NOT_IN_SCHEDULE", "Bác sĩ không có ca làm việc ngày này");
        }

        WorkingHours.Shift matchingShift = null;
        LocalTime matchedEndTime = null;

        for (WorkingHours.Shift shift : workingHours.shifts()) {
            int slotMinutes = shift.slotMinutes() > 0 ? shift.slotMinutes() : 20;
            LocalTime cur = shift.startTime();
            while (!cur.plusMinutes(slotMinutes).isAfter(shift.endTime())) {
                LocalTime next = cur.plusMinutes(slotMinutes);
                if (cur.equals(time)) {
                    matchingShift = shift;
                    matchedEndTime = next;
                    break;
                }
                cur = next;
            }
            if (matchingShift != null) break;
        }

        if (matchingShift == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "SLOT_NOT_IN_SCHEDULE",
                "Giờ khám không khớp với bất kỳ khung giờ nào trong ca làm việc");
        }

        LocalDateTime endTime = LocalDateTime.of(date, matchedEndTime);

        // 6. Kiểm tra hồ sơ có lịch chồng giờ không (CS12: khoảng [start, end) nửa mở)
        List<Appointment> overlapping = appointmentRepository.findOverlappingForPatient(
            request.patientId(),
            List.of(AppointmentStatus.HELD, AppointmentStatus.CONFIRMED),
            request.startTime(),
            endTime
        );
        if (!overlapping.isEmpty()) {
            throw new BusinessException(HttpStatus.CONFLICT, "PATIENT_TIME_CONFLICT",
                "Bệnh nhân đã có lịch hẹn khác trong khung giờ này");
        }

        // 7. Tạo bản ghi HELD
        Appointment appointment = new Appointment();
        appointment.setPatientId(request.patientId());
        appointment.setDoctorId(request.doctorId());
        appointment.setRoomId(matchingShift.roomId());
        appointment.setWorkScheduleId(matchingShift.workScheduleId());
        appointment.setBookedByAccountId(accountId);
        appointment.setStartTime(request.startTime());
        appointment.setEndTime(endTime);
        appointment.setStatus(AppointmentStatus.HELD);
        appointment.setHoldExpiresAt(now.plusMinutes(holdMinutes));
        appointment.setReason(request.reason());

        try {
            appointment = appointmentRepository.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(HttpStatus.CONFLICT, "SLOT_ALREADY_HELD", "Khung giờ này vừa có người giữ chỗ");
        }

        return AppointmentResponse.from(appointment);
    }

    @Transactional
    public AppointmentResponse confirm(Long id, Long accountId) {
        Appointment appointment = appointmentRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("Lịch hẹn", id));

        // Chỉ người đã giữ chỗ mới được xác nhận
        if (!appointment.getBookedByAccountId().equals(accountId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Chỉ người giữ chỗ mới được xác nhận lịch hẹn");
        }

        if (appointment.getStatus() != AppointmentStatus.HELD) {
            throw new BusinessException(HttpStatus.CONFLICT, "INVALID_STATUS", "Lịch hẹn không ở trạng thái giữ chỗ");
        }

        LocalDateTime now = LocalDateTime.now();
        // CS02: Quá hạn khi now > holdExpiresAt; đúng mốc vẫn xác nhận được
        if (appointment.getHoldExpiresAt() != null && now.isAfter(appointment.getHoldExpiresAt())) {
            appointment.setStatus(AppointmentStatus.EXPIRED);
            appointmentRepository.save(appointment);
            throw new BusinessException(HttpStatus.CONFLICT, "HOLD_EXPIRED", "Đã hết thời gian giữ chỗ (quá 5 phút)");
        }

        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment.setConfirmedAt(now);
        appointment.setHoldExpiresAt(null);
        appointment = appointmentRepository.save(appointment);

        // Phát sự kiện appointment.confirmed sau khi lưu database thành công
        eventPublisher.publishConfirmed(
            appointment.getId(),
            appointment.getPatientId(),
            appointment.getDoctorId(),
            appointment.getRoomId(),
            appointment.getStartTime(),
            appointment.getEndTime(),
            accountId
        );

        return AppointmentResponse.from(appointment);
    }

    @Transactional
    public AppointmentResponse cancel(Long id, String reason, Long accountId, String role) {
        Appointment appointment = appointmentRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("Lịch hẹn", id));

        // CS08: Ai được hủy: Người đặt và Lễ tân; Bác sĩ không được hủy
        if ("DOCTOR".equals(role)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Bác sĩ không được quyền hủy lịch hẹn");
        }

        boolean isBooker = appointment.getBookedByAccountId().equals(accountId);
        boolean isStaff = "ADMIN".equals(role) || "RECEPTIONIST".equals(role);

        if (!isBooker && !isStaff) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Bạn không có quyền hủy lịch hẹn này");
        }

        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new BusinessException(HttpStatus.CONFLICT, "INVALID_STATUS", "Chỉ có thể hủy lịch hẹn đã xác nhận");
        }

        LocalDateTime now = LocalDateTime.now();

        // CS07: Bệnh nhân hủy khi còn dưới cancelBeforeHours (2 giờ) -> 409 TOO_LATE_TO_CANCEL
        // Lễ tân hủy được tới giờ bắt đầu
        if (!isStaff) {
            if (now.isAfter(appointment.getStartTime().minusHours(cancelBeforeHours))) {
                throw new BusinessException(HttpStatus.CONFLICT, "TOO_LATE_TO_CANCEL",
                    "Bệnh nhân chỉ được hủy trước giờ khám ít nhất " + cancelBeforeHours + " giờ");
            }
        } else {
            if (now.isAfter(appointment.getStartTime())) {
                throw new BusinessException(HttpStatus.CONFLICT, "TOO_LATE_TO_CANCEL",
                    "Không thể hủy lịch khám đã qua giờ bắt đầu");
            }
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelReason(reason);
        appointment.setCancelledByAccountId(accountId);
        appointment.setCancelledAt(now);
        appointment = appointmentRepository.save(appointment);

        // Phát sự kiện appointment.cancelled sau khi lưu database thành công
        eventPublisher.publishCancelled(
            appointment.getId(),
            appointment.getPatientId(),
            appointment.getDoctorId(),
            appointment.getRoomId(),
            appointment.getStartTime(),
            appointment.getEndTime(),
            reason,
            accountId
        );

        return AppointmentResponse.from(appointment);
    }

    @Transactional
    public AppointmentResponse complete(Long id, Long accountId, Long doctorId, String role) {
        Appointment appointment = appointmentRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("Lịch hẹn", id));

        // DOCTOR hoặc RECEPTIONIST/ADMIN
        if ("DOCTOR".equals(role)) {
            if (doctorId == null || !appointment.getDoctorId().equals(doctorId)) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Bác sĩ chỉ được đánh dấu lịch khám của chính mình");
            }
        } else if (!"ADMIN".equals(role) && !"RECEPTIONIST".equals(role)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Bạn không có quyền đánh dấu đã khám");
        }

        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new BusinessException(HttpStatus.CONFLICT, "INVALID_STATUS", "Chỉ có thể đánh dấu lịch đã xác nhận");
        }

        LocalDateTime now = LocalDateTime.now();
        // CS09: Chỉ từ giờ bắt đầu trở đi mới được đánh dấu đã khám
        if (now.isBefore(appointment.getStartTime())) {
            throw new BusinessException(HttpStatus.CONFLICT, "TOO_EARLY_TO_COMPLETE",
                "Chỉ được đánh dấu đã khám từ giờ bắt đầu khám trở đi");
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setCompletedAt(now);
        appointment.setCompletedByAccountId(accountId);
        appointment = appointmentRepository.save(appointment);

        return AppointmentResponse.from(appointment);
    }

    @Transactional
    public AppointmentResponse noShow(Long id, Long accountId, Long doctorId, String role) {
        Appointment appointment = appointmentRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("Lịch hẹn", id));

        // CS10: DOCTOR hoặc RECEPTIONIST/ADMIN
        if ("DOCTOR".equals(role)) {
            if (doctorId == null || !appointment.getDoctorId().equals(doctorId)) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Bác sĩ chỉ được ghi không đến cho lịch của mình");
            }
        } else if (!"ADMIN".equals(role) && !"RECEPTIONIST".equals(role)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Bạn không có quyền ghi bệnh nhân không đến");
        }

        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new BusinessException(HttpStatus.CONFLICT, "INVALID_STATUS", "Chỉ áp dụng cho lịch đã xác nhận");
        }

        LocalDateTime now = LocalDateTime.now();
        // CS10: Chỉ sau giờ bắt đầu
        if (now.isBefore(appointment.getStartTime())) {
            throw new BusinessException(HttpStatus.CONFLICT, "TOO_EARLY_FOR_NO_SHOW",
                "Chỉ được ghi không đến sau khi đã qua giờ bắt đầu khám");
        }

        appointment.setStatus(AppointmentStatus.NO_SHOW);
        appointment.setNoShowAt(now);
        appointment.setNoShowByAccountId(accountId);
        appointment = appointmentRepository.save(appointment);

        return AppointmentResponse.from(appointment);
    }

    public AppointmentResponse get(Long id, Long accountId, String role, Long doctorId) {
        Appointment appointment = appointmentRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("Lịch hẹn", id));

        if ("ADMIN".equals(role) || "RECEPTIONIST".equals(role)) {
            return AppointmentResponse.from(appointment);
        }

        if ("DOCTOR".equals(role)) {
            if (doctorId != null && appointment.getDoctorId().equals(doctorId)) {
                return AppointmentResponse.from(appointment);
            }
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Bác sĩ chỉ được xem lịch của mình");
        }

        if (appointment.getBookedByAccountId().equals(accountId)) {
            return AppointmentResponse.from(appointment);
        }

        throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Bạn không có quyền xem lịch hẹn này");
    }

    public List<AppointmentResponse> listMine(Long accountId) {
        return appointmentRepository.findByBookedByAccountIdOrderByStartTimeDesc(accountId)
            .stream()
            .map(AppointmentResponse::from)
            .toList();
    }

    public List<AppointmentResponse> listByDoctor(Long doctorId, LocalDate date, AppointmentStatus status) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        List<Appointment> appointments;
        if (status != null) {
            appointments = appointmentRepository.findByDoctorIdAndStatusInAndStartTimeBetween(
                doctorId, List.of(status), startOfDay, endOfDay);
        } else {
            appointments = appointmentRepository.findByDoctorIdAndStartTimeBetweenOrderByStartTime(
                doctorId, startOfDay, endOfDay);
        }

        return appointments.stream()
            .map(AppointmentResponse::from)
            .toList();
    }

    public List<AppointmentResponse> search(Long bookedByAccountId, List<Long> patientIds, Long doctorId,
                                            AppointmentStatus status, LocalDateTime from, LocalDateTime to) {
        Specification<Appointment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (bookedByAccountId != null && patientIds != null && !patientIds.isEmpty()) {
                Predicate byAccount = cb.equal(root.get("bookedByAccountId"), bookedByAccountId);
                Predicate byPatients = root.get("patientId").in(patientIds);
                predicates.add(cb.or(byAccount, byPatients));
            } else if (bookedByAccountId != null) {
                predicates.add(cb.equal(root.get("bookedByAccountId"), bookedByAccountId));
            } else if (patientIds != null && !patientIds.isEmpty()) {
                predicates.add(root.get("patientId").in(patientIds));
            }

            if (doctorId != null) {
                predicates.add(cb.equal(root.get("doctorId"), doctorId));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startTime"), from));
            }

            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startTime"), to));
            }

            if (query != null) {
                query.orderBy(cb.desc(root.get("startTime")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return appointmentRepository.findAll(spec)
            .stream()
            .map(AppointmentResponse::from)
            .toList();
    }

    @Transactional
    public int expireHolds() {
        return appointmentRepository.expireHolds(LocalDateTime.now());
    }
}
