package com.clinic.reminder.job;

import com.clinic.reminder.client.PatientClient;
import com.clinic.reminder.client.SchedulingClient;
import com.clinic.reminder.common.NotImplementedYetException;
import com.clinic.reminder.log.ReminderLogRepository;
import com.clinic.reminder.log.dto.RunResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class ReminderService {

    private final SchedulingClient schedulingClient;
    private final PatientClient patientClient;
    private final ReminderLogRepository reminderLogRepository;
    private final JavaMailSender mailSender;
    private final int hoursBefore;
    private final String fromAddress;

    public ReminderService(SchedulingClient schedulingClient, PatientClient patientClient,
                           ReminderLogRepository reminderLogRepository, JavaMailSender mailSender,
                           @Value("${reminder.hours-before}") int hoursBefore,
                           @Value("${reminder.from-address}") String fromAddress) {
        this.schedulingClient = schedulingClient;
        this.patientClient = patientClient;
        this.reminderLogRepository = reminderLogRepository;
        this.mailSender = mailSender;
        this.hoursBefore = hoursBefore;
        this.fromAddress = fromAddress;
    }

    public RunResult runOnce() {
        throw new NotImplementedYetException(
            "lấy lịch CONFIRMED trong hoursBefore giờ tới, bỏ lịch đã có log SENT/SKIPPED, lấy email từ Patient, "
                + "gửi mail, ghi reminder_logs (FAILED thì lần sau thử lại, tăng attempt_count)");
    }
}
