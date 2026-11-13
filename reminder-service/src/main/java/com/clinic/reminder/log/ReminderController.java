package com.clinic.reminder.log;

import com.clinic.reminder.common.BusinessException;
import com.clinic.reminder.job.ReminderService;
import com.clinic.reminder.log.dto.ReminderLogResponse;
import com.clinic.reminder.log.dto.RunResult;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
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
    public List<ReminderLogResponse> logs(
            @RequestParam(required = false) ReminderStatus status,
            @RequestParam(required = false) ReminderType type,
            @RequestHeader(value = "X-User-Role", required = false) String role) {

        if (role != null && !("ADMIN".equals(role) || "RECEPTIONIST".equals(role))) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Không có quyền xem nhật ký nhắc lịch");
        }

        return reminderLogRepository.findLogs(status, type, PageRequest.of(0, 100))
                .stream()
                .map(ReminderLogResponse::from)
                .toList();
    }

    // Chạy quét ngay, không chờ cron — tiện khi demo
    @PostMapping("/run")
    public RunResult runNow(@RequestHeader(value = "X-User-Role", required = false) String role) {
        if (role != null && !"ADMIN".equals(role)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Chỉ quản trị viên mới được yêu cầu quét ngay");
        }
        return reminderService.runOnce();
    }
}
