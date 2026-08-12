package ru.bulbasaur.office.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.bulbasaur.office.infra.persistence.entity.PokerTaskEntity;

import java.util.List;
import java.util.UUID;

public interface PokerTaskJpaRepository extends JpaRepository<PokerTaskEntity, UUID> {

    List<PokerTaskEntity> findByRoomIdOrderByCreatedAtAsc(UUID roomId);
}
