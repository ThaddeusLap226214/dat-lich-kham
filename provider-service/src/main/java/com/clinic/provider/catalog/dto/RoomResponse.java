package com.clinic.provider.catalog.dto;

import com.clinic.provider.catalog.Room;

public record RoomResponse(Long id, String code, String name, String location, boolean active) {

    public static RoomResponse from(Room r) {
        return new RoomResponse(r.getId(), r.getCode(), r.getName(), r.getLocation(), r.isActive());
    }
}
