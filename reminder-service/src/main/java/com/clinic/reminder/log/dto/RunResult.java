package com.clinic.reminder.log.dto;

public record RunResult(int checked, int sent, int skipped, int failed) {
}
