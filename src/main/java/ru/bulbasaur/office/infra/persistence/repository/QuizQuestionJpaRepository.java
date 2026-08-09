package ru.bulbasaur.office.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.bulbasaur.office.infra.persistence.entity.QuizQuestionEntity;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface QuizQuestionJpaRepository extends JpaRepository<QuizQuestionEntity, UUID> {
    List<QuizQuestionEntity> findByTopicCode(String topicCode);

    List<QuizQuestionEntity> findByTopicCodeAndIdNotIn(String topicCode, Collection<UUID> ids);
}
