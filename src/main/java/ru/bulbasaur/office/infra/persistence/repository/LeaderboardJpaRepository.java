package ru.bulbasaur.office.infra.persistence.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.bulbasaur.office.domain.model.GameId;
import ru.bulbasaur.office.infra.persistence.entity.LeaderboardEntryEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaderboardJpaRepository extends JpaRepository<LeaderboardEntryEntity, UUID> {

    /**
     * Вставить результат, а при конфликте (уже есть строка игрока/игры) — обновить
     * только если новое значение лучше по правилу higherBetter.
     */
    @Modifying
    @Query(value = """
            insert into leaderboard (player_id, game, value, updated_at)
            values (:playerId, :game, :value, now())
            on conflict (player_id, game) do update
                set value = excluded.value, updated_at = excluded.updated_at
                where (:higherBetter and excluded.value > leaderboard.value)
                   or (not :higherBetter and excluded.value < leaderboard.value)
            """, nativeQuery = true)
    void upsertBest(@Param("playerId") UUID playerId,
                    @Param("game") String game,
                    @Param("value") long value,
                    @Param("higherBetter") boolean higherBetter);

    /** Накопительно: при конфликте прибавляет value к текущему значению (для wordle). */
    @Modifying
    @Query(value = """
            insert into leaderboard (player_id, game, value, updated_at)
            values (:playerId, :game, :value, now())
            on conflict (player_id, game) do update
                set value = leaderboard.value + excluded.value, updated_at = excluded.updated_at
            """, nativeQuery = true)
    void upsertAdd(@Param("playerId") UUID playerId,
                   @Param("game") String game,
                   @Param("value") long value);

    @Query("""
            select p.login as login, p.id as playerId, l.value as value
            from LeaderboardEntryEntity l
            join l.player p
            where l.game = :game
            """)
    List<LeaderboardRowProjection> findRows(@Param("game") GameId game, Pageable pageable);

    Optional<LeaderboardEntryEntity> findByPlayer_IdAndGame(UUID playerId, GameId game);

    long countByGameAndValueGreaterThan(GameId game, long value);

    long countByGameAndValueLessThan(GameId game, long value);
}
