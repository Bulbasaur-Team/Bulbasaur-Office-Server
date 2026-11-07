package ru.bulbasaur.office.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.infra.persistence.entity.JumpSeasonEntity;

import java.util.Optional;
import java.util.UUID;

public interface JumpSeasonJpaRepository extends JpaRepository<JumpSeasonEntity, UUID> {

    Optional<JumpSeasonEntity> findBySeasonNumber(int seasonNumber);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("delete from JumpSeasonEntity s where s.seasonNumber > :number")
    void deleteBySeasonNumberGreaterThan(@Param("number") int number);
}
