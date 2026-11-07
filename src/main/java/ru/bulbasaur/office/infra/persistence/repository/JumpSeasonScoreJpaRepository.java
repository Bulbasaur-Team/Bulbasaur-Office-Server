package ru.bulbasaur.office.infra.persistence.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.bulbasaur.office.infra.persistence.entity.JumpSeasonScoreEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JumpSeasonScoreJpaRepository extends JpaRepository<JumpSeasonScoreEntity, UUID> {

    @Modifying
    @Query(value = """
            insert into jump_season_score (id, season_id, player_id, value, updated_at)
            values (gen_random_uuid(), :seasonId, :playerId, :value, now())
            on conflict (season_id, player_id) do update
                set value = excluded.value, updated_at = excluded.updated_at
                where excluded.value > jump_season_score.value
            """, nativeQuery = true)
    void upsertBest(@Param("seasonId") UUID seasonId,
                    @Param("playerId") UUID playerId,
                    @Param("value") long value);

    @Query("""
            select p.login as login, p.id as playerId, s.value as value
            from JumpSeasonScoreEntity s
            join s.player p
            where s.season.id = :seasonId
            """)
    List<LeaderboardRowProjection> findRows(@Param("seasonId") UUID seasonId, Pageable pageable);

    @Query("""
            select s.value from JumpSeasonScoreEntity s
            where s.season.id = :seasonId and s.player.id = :playerId
            """)
    Optional<Long> findValue(@Param("seasonId") UUID seasonId, @Param("playerId") UUID playerId);

    @Query("""
            select count(s) from JumpSeasonScoreEntity s
            where s.season.id = :seasonId and s.value > :value
            """)
    long countBetter(@Param("seasonId") UUID seasonId, @Param("value") long value);

    @Query("""
            select p.login as login, p.id as playerId, s.value as value, season.seasonNumber as seasonNumber
            from JumpSeasonScoreEntity s
            join s.player p
            join s.season season
            where season.awardedAt is not null
            order by season.seasonNumber desc, s.value desc, s.updatedAt asc
            """)
    List<JumpSeasonWinnerProjection> findPreviousWinners(Pageable pageable);
}
