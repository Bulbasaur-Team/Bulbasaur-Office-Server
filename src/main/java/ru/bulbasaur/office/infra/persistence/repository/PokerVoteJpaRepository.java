package ru.bulbasaur.office.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.bulbasaur.office.infra.persistence.entity.PokerVoteEntity;

import java.util.UUID;

public interface PokerVoteJpaRepository extends JpaRepository<PokerVoteEntity, UUID> {
}
