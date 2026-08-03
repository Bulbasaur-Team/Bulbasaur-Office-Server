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

    @Modifying
    @Query("""
            update PlayerEntity p
            set p.bulbaCoinBalance = p.bulbaCoinBalance + :delta
            where p.id = :id
            """)
    int addBalance(@Param("id") UUID id, @Param("delta") long delta);

    @Modifying
    @Query("""
            update PlayerEntity p
            set p.bulbaCoinBalance = p.bulbaCoinBalance - :amount
            where p.id = :id and p.bulbaCoinBalance >= :amount
            """)
    int subtractBalanceIfEnough(@Param("id") UUID id, @Param("amount") long amount);

    @Query("select p.bulbaCoinBalance from PlayerEntity p where p.id = :id")
    Optional<Long> findBalance(@Param("id") UUID id);

    /** Игроки по убыванию числа ачивок, при равенстве — по дате регистрации. */
    @Query("""
            select p.id as id, p.login as login, count(a.id) as owned
            from PlayerEntity p
            left join AchievementEntity a on a.playerId = p.id and a.code not in :secretCodes
            group by p.id, p.login, p.createdAt
            order by count(a.id) desc, p.createdAt
            """)
    List<CommunityRowProjection> findCommunityRows(@Param("secretCodes") List<String> secretCodes);
}
