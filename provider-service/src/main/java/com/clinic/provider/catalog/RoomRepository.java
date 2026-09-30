package com.clinic.provider.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByIdIn(Collection<Long> ids);

    boolean existsByCode(String code);
}
