package com.clinic.reminder.log;

import com.clinic.reminder.common.NotImplementedYetException;
import com.clinic.reminder.job.ReminderService;
import com.clinic.reminder.log.dto.ReminderLogResponse;
import com.clinic.reminder.log.dto.RunResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reminders")
public class ReminderController {

    private final ReminderLogRepository reminderLogRepository;
    private final ReminderService reminderService;

    public ReminderController(ReminderLogRepository reminderLogRepository, ReminderService reminderService) {
        this.reminderLogRepository = reminderLogRepository;
        this.reminderService = reminderService;
    }

    @GetMapping("/logs")
    public List<ReminderLogResponse> logs(@RequestParam(required = false) ReminderStatus status) {
        throw new NotImplementedYetException("100 dòng nhật ký gần nhất, lọc theo trạng thái nếu có");
    }

    // Chạy quét ngay, không chờ cron — tiện khi demo
    @PostMapping("/run")
    public RunResult runNow() {
        return reminderService.runOnce();
    }
}
