package com.clinic.provider.catalog.dto;

import com.clinic.provider.catalog.Specialty;

public record SpecialtyResponse(Long id, String code, String name, String description, boolean active) {

    public static SpecialtyResponse from(Specialty s) {
        return new SpecialtyResponse(s.getId(), s.getCode(), s.getName(), s.getDescription(), s.isActive());
    }
}
