package ru.bulbasaur.office.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.bulbasaur.office.infra.persistence.entity.RetroRoomEntity;

import java.util.List;
import java.util.UUID;

public interface RetroRoomJpaRepository extends JpaRepository<RetroRoomEntity, UUID> {

    List<RetroRoomEntity> findByStatusOrderByCreatedAtDesc(String status);

    long countByStatus(String status);
}
