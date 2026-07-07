package ru.bulbasaur.office.infra.persistence.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.bulbasaur.office.domain.model.GameId;
import ru.bulbasaur.office.infra.persistence.entity.WotdProgressEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WotdProgressJpaRepository extends JpaRepository<WotdProgressEntity, UUID> {

    Optional<WotdProgressEntity> findByPlayer_IdAndGameAndDay(UUID playerId, GameId game, LocalDate day);

    @Query("""
            select p.login as login, p.id as playerId, l.attempts as value
            from WotdProgressEntity l
            join l.player p
            where l.game = :game and l.day = :day and l.solved = true
            """)
    List<LeaderboardRowProjection> findSolvedRows(@Param("game") GameId game,
                                                  @Param("day") LocalDate day,
                                                  Pageable pageable);

    long countByGameAndDayAndSolvedTrueAndAttemptsLessThan(GameId game, LocalDate day, int attempts);
}
