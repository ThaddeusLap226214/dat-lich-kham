package com.clinic.patient.duplicate.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// keepPatientId: hồ sơ được giữ lại (một trong hai hồ sơ của cặp), hồ sơ còn lại chuyển sang MERGED
public record MergeRequest(@NotNull Long keepPatientId, @Size(max = 500) String note) {
}
