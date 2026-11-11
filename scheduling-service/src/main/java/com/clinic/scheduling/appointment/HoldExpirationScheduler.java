package com.clinic.scheduling.appointment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class HoldExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(HoldExpirationScheduler.class);
    private final AppointmentService appointmentService;

    public HoldExpirationScheduler(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    // Chạy định kỳ mỗi 10 giây để nhả các chỗ HELD quá hạn (F34)
    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.SECONDS)
    public void expireHolds() {
        try {
            int expired = appointmentService.expireHolds();
            if (expired > 0) {
                log.info("Đã nhả {} chỗ giữ quá hạn", expired);
            }
        } catch (Exception e) {
            log.error("Lỗi khi nhả chỗ giữ quá hạn: {}", e.getMessage(), e);
        }
    }
}
