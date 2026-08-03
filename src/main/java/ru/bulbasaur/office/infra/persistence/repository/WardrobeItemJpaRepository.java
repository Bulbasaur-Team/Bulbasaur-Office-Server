package ru.bulbasaur.office.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.bulbasaur.office.infra.persistence.entity.WardrobeItemEntity;

import java.util.List;

public interface WardrobeItemJpaRepository extends JpaRepository<WardrobeItemEntity, String> {

    List<WardrobeItemEntity> findAllByOrderBySortOrderAsc();
}
