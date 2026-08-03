package ru.bulbasaur.office.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.bulbasaur.office.infra.persistence.entity.WardrobeItemEntity;

public interface WardrobeItemJpaRepository extends JpaRepository<WardrobeItemEntity, String> {
}
