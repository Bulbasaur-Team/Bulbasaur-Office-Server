package ru.bulbasaur.office.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.bulbasaur.office.infra.persistence.entity.QuizTopicEntity;

import java.util.List;

public interface QuizTopicJpaRepository extends JpaRepository<QuizTopicEntity, String> {
    List<QuizTopicEntity> findAllByOrderBySortOrderAsc();
}
