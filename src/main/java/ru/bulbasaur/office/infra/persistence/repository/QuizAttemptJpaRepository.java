package ru.bulbasaur.office.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.bulbasaur.office.infra.persistence.entity.QuizAttemptEntity;

import java.util.List;
import java.util.UUID;

public interface QuizAttemptJpaRepository extends JpaRepository<QuizAttemptEntity, UUID> {
    List<QuizAttemptEntity> findByPlayerIdAndStatus(UUID playerId, String status);
}
