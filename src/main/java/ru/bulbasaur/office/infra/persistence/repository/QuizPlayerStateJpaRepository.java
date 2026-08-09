package ru.bulbasaur.office.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.bulbasaur.office.infra.persistence.entity.QuizPlayerStateEntity;

import java.util.UUID;

public interface QuizPlayerStateJpaRepository extends JpaRepository<QuizPlayerStateEntity, UUID> {
}
