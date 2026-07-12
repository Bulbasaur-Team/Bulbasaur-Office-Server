package ru.bulbasaur.office.infra.persistence.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.bulbasaur.office.infra.persistence.entity.EventLogEntity;

import java.util.List;
import java.util.UUID;

public interface EventLogJpaRepository extends JpaRepository<EventLogEntity, UUID> {

    List<EventLogEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
