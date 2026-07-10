package ru.bulbasaur.office.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.bulbasaur.office.infra.persistence.entity.PlayerEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerJpaRepository extends JpaRepository<PlayerEntity, UUID> {

    Optional<PlayerEntity> findByLogin(String login);

    boolean existsByLogin(String login);

    @Query("select p.id from PlayerEntity p")
    List<UUID> findAllIds();

    @Modifying
    @Query("delete from PlayerEntity p where p.id = :id")
    void deleteAccount(@Param("id") UUID id);
}
