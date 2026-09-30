package com.clinic.reminder.job;

import com.clinic.reminder.common.NotImplementedYetException;
import com.clinic.reminder.log.dto.RunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReminderJob {

    private static final Logger log = LoggerFactory.getLogger(ReminderJob.class);

    private final ReminderService reminderService;

    public ReminderJob(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @Scheduled(cron = "${reminder.cron}")
    public void run() {
        try {
            RunResult result = reminderService.runOnce();
            log.info("Quét nhắc lịch: {}", result);
        } catch (NotImplementedYetException e) {
            log.warn(e.getMessage());
        } catch (Exception e) {
            // Scheduling / Patient tạm tắt thì lần quét sau thử lại, không làm sập Reminder
            log.error("Quét nhắc lịch lỗi, sẽ thử lại ở lần chạy sau", e);
        }
    }
}
