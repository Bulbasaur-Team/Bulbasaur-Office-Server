package ru.bulbasaur.office.infra.persistence.connector;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.domain.model.JumpSeason;
import ru.bulbasaur.office.domain.model.JumpSeasonCalendar;
import ru.bulbasaur.office.domain.model.LeaderboardRow;
import ru.bulbasaur.office.domain.model.SeasonChampion;
import ru.bulbasaur.office.infra.persistence.entity.JumpSeasonEntity;
import ru.bulbasaur.office.infra.persistence.repository.JumpSeasonScoreJpaRepository;
import ru.bulbasaur.office.usecase.port.out.JumpSeasonRepositoryPort;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JumpSeasonConnector implements JumpSeasonRepositoryPort {

    private final EntityManager em;
    private final JumpSeasonScoreJpaRepository scores;

    @Override
    @Transactional
    public JumpSeason lockLatest() {
        JumpSeasonEntity entity = em.createQuery(
                        "select s from JumpSeasonEntity s order by s.seasonNumber desc",
                        JumpSeasonEntity.class)
                .setMaxResults(1)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getSingleResult();
        return toDomain(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<JumpSeason> findByNumber(int seasonNumber) {
        List<JumpSeasonEntity> found = em.createQuery(
                        "select s from JumpSeasonEntity s where s.seasonNumber = :number",
                        JumpSeasonEntity.class)
                .setParameter("number", seasonNumber)
                .setMaxResults(1)
                .getResultList();
        return found.stream().findFirst().map(this::toDomain);
    }

    @Override
    @Transactional
    public void markAwarded(UUID seasonId, Instant awardedAt) {
        JumpSeasonEntity season = em.find(JumpSeasonEntity.class, seasonId);
        if (season == null) {
            throw new IllegalStateException("Сезон не найден");
        }
        if (season.getAwardedAt() == null) {
            season.setAwardedAt(awardedAt);
        }
        // Сразу в БД: следующий insert не должен увидеть второй незакрытый сезон.
        em.flush();
    }

    @Override
    @Transactional
    public JumpSeason openNext(JumpSeason previous) {
        JumpSeasonEntity prev = em.find(JumpSeasonEntity.class, previous.id());
        if (prev == null || prev.getAwardedAt() == null) {
            throw new IllegalStateException("Новый сезон можно открыть только после завершения предыдущего");
        }
        int number = prev.getSeasonNumber() + 1;
        // Стык в стык с фактическим концом. У первого сезона он совпадает с календарём,
        // у сдвинутого для проверки — следующий начинается сразу, а не через недели.
        Instant startsAt = prev.getEndsAt();
        JumpSeasonEntity created = new JumpSeasonEntity();
        created.setId(UUID.randomUUID());
        created.setSeasonNumber(number);
        created.setStartsAt(startsAt);
        created.setEndsAt(startsAt.plus(JumpSeasonCalendar.LENGTH));
        em.persist(created);
        em.flush();
        return toDomain(created);
    }

    @Override
    @Transactional
    public void upsertScore(UUID seasonId, UUID playerId, long value) {
        scores.upsertBest(seasonId, playerId, value);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaderboardRow> top(UUID seasonId, int limit) {
        Sort sort = Sort.by(Sort.Order.desc("value"), Sort.Order.asc("updatedAt"));
        return scores.findRows(seasonId, PageRequest.of(0, limit, sort)).stream()
                .map(row -> new LeaderboardRow(row.getLogin(), row.getPlayerId(), row.getValue()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> valueOf(UUID seasonId, UUID playerId) {
        return scores.findValue(seasonId, playerId);
    }

    @Override
    @Transactional(readOnly = true)
    public long betterCount(UUID seasonId, long value) {
        return scores.countBetter(seasonId, value);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SeasonChampion> previousWinner() {
        return scores.findPreviousWinners(PageRequest.of(0, 1)).stream()
                .findFirst()
                .map(row -> new SeasonChampion(
                        row.getSeasonNumber(), row.getLogin(), row.getPlayerId(), row.getValue()));
    }

    private JumpSeason toDomain(JumpSeasonEntity entity) {
        return new JumpSeason(
                entity.getId(),
                entity.getSeasonNumber(),
                entity.getStartsAt(),
                entity.getEndsAt(),
                entity.getAwardedAt());
    }
}
