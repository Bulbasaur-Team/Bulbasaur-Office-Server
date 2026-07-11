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

    boolean existsByPlayer_IdAndGameAndSolvedTrue(UUID playerId, GameId game);

    @Query("""
            select w.day from WotdProgressEntity w
            where w.player.id = :playerId and w.game = :game and w.solved = true
            order by w.day
            """)
    List<LocalDate> findSolvedDays(@Param("playerId") UUID playerId, @Param("game") GameId game);

    /**
     * Разгадал ли игрок слово дня раньше всех хотя бы в один день: есть день, где он
     * решил слово и никто другой не решил его раньше по времени (число попыток не важно).
     */
    @Query(value = """
            select exists(
                select 1 from wotd_progress w
                where w.player_id = :playerId and w.game = :game and w.solved = true
                  and not exists (
                      select 1 from wotd_progress o
                      where o.game = w.game and o.day = w.day and o.solved = true
                        and o.player_id <> w.player_id
                        and o.updated_at < w.updated_at
                  )
            )
            """, nativeQuery = true)
    boolean wasEverFirstToSolve(@Param("playerId") UUID playerId, @Param("game") String game);
}
