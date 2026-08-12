package ru.bulbasaur.office.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.bulbasaur.office.infra.persistence.entity.PokerRoomEntity;

import java.util.List;
import java.util.UUID;

public interface PokerRoomJpaRepository extends JpaRepository<PokerRoomEntity, UUID> {

    List<PokerRoomEntity> findByStatusOrderByCreatedAtDesc(String status);

    long countByStatus(String status);
}
