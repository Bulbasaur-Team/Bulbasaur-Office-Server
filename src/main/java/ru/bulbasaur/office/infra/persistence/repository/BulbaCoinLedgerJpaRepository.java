package ru.bulbasaur.office.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.bulbasaur.office.infra.persistence.entity.BulbaCoinLedgerEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface BulbaCoinLedgerJpaRepository extends JpaRepository<BulbaCoinLedgerEntity, UUID> {

    @Modifying
    @Query(value = """
            insert into bulba_coin_ledger (id, player_id, amount, kind, ref, title, created_at)
            values (gen_random_uuid(), :playerId, :amount, :kind, :ref, :title, now())
            on conflict (player_id, kind, ref) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(@Param("playerId") UUID playerId,
                        @Param("amount") long amount,
                        @Param("kind") String kind,
                        @Param("ref") String ref,
                        @Param("title") String title);

    @Query("""
            select e from BulbaCoinLedgerEntity e
            where e.playerId = :playerId
            order by e.createdAt desc
            """)
    List<BulbaCoinLedgerEntity> findHistory(@Param("playerId") UUID playerId,
                                            org.springframework.data.domain.Pageable pageable);

    @Query("""
            select e from BulbaCoinLedgerEntity e
            where e.playerId = :playerId
              and e.createdAt < :before
            order by e.createdAt desc
            """)
    List<BulbaCoinLedgerEntity> findHistoryBefore(@Param("playerId") UUID playerId,
                                                  @Param("before") Instant before,
                                                  org.springframework.data.domain.Pageable pageable);
}
