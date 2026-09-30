package com.clinic.scheduling.slot;

import java.time.LocalDateTime;

public record SlotResponse(LocalDateTime startTime, LocalDateTime endTime, Long roomId, Long workScheduleId,
                           boolean available) {
}
