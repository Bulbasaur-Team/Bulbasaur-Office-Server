package ru.bulbasaur.office.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.bulbasaur.office.infra.persistence.entity.RetroStickerGroupEntity;

import java.util.UUID;

public interface RetroStickerGroupJpaRepository extends JpaRepository<RetroStickerGroupEntity, UUID> {
}
