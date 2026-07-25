package ru.bulbasaur.office.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.bulbasaur.office.infra.persistence.entity.RetroMoodEntity;

import java.util.List;
import java.util.UUID;

public interface RetroMoodJpaRepository extends JpaRepository<RetroMoodEntity, RetroMoodEntity.Pk> {

    List<RetroMoodEntity> findByRoomId(UUID roomId);
}
