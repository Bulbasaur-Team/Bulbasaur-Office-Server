package ru.bulbasaur.office.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.bulbasaur.office.infra.persistence.entity.PlayerPresentationDeckEntity;

import java.util.UUID;

public interface PlayerPresentationDeckJpaRepository extends JpaRepository<PlayerPresentationDeckEntity, UUID> {
}
