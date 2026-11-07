package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.domain.model.BulbaCoinKind;
import ru.bulbasaur.office.domain.model.JumpSeason;
import ru.bulbasaur.office.domain.model.JumpSeasonCalendar;
import ru.bulbasaur.office.domain.model.LeaderboardRow;
import ru.bulbasaur.office.usecase.dto.JumpSeasonPrizeLine;
import ru.bulbasaur.office.usecase.dto.JumpSeasonView;
import ru.bulbasaur.office.usecase.dto.LeaderboardRowView;
import ru.bulbasaur.office.usecase.port.out.JumpSeasonRepositoryPort;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Сезоны Bulba Jump. В зачёт идёт лучший результат, набранный внутри окна сезона.
 * Следующий сезон открывается только после того, как предыдущий завершился и призы выданы.
 */
@Service
@RequiredArgsConstructor
public class JumpSeasonUsecase {

    private final JumpSeasonRepositoryPort seasons;
    private final CreditBulbaCoinsUsecase credit;
    private final EventLogService eventLog;

    @Transactional
    public JumpSeasonView current(UUID playerId, String login, int limit, Instant now) {
        JumpSeason season = catchUp(now);
        List<LeaderboardRow> rows = seasons.top(season.id(), limit);
        List<LeaderboardRowView> top = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            LeaderboardRow row = rows.get(i);
            top.add(new LeaderboardRowView(i + 1, row.login(), row.value(), row.playerId().equals(playerId)));
        }

        LeaderboardRowView me = null;
        Optional<Long> myValue = seasons.valueOf(season.id(), playerId);
        if (myValue.isPresent()) {
            long value = myValue.get();
            int rank = (int) seasons.betterCount(season.id(), value) + 1;
            me = new LeaderboardRowView(rank, login, value, true);
        }

        JumpSeasonView.PreviousWinner previous = seasons.previousWinner()
                .map(row -> new JumpSeasonView.PreviousWinner(row.seasonNumber(), row.login(), row.value()))
                .orElse(new JumpSeasonView.PreviousWinner(
                        0,
                        JumpSeasonCalendar.LEGACY_WINNER_LOGIN,
                        JumpSeasonCalendar.LEGACY_WINNER_SCORE));

        return new JumpSeasonView(
                season.seasonNumber(),
                season.startsAt(),
                season.endsAt(),
                now,
                JumpSeasonCalendar.PRIZES,
                previous,
                top,
                me);
    }

    /** Записать результат, если момент попадает в открытый сезон. Худший повтор не затирает рекорд. */
    @Transactional
    public void record(UUID playerId, long value, Instant now) {
        JumpSeason season = catchUp(now);
        if (!season.contains(now)) {
            return;
        }
        seasons.upsertScore(season.id(), playerId, value);
    }

    /**
     * Закрыть сезоны, у которых уже наступил {@code endsAt}: выдать призы и открыть следующий.
     * Срок берётся из строки сезона, а не из календарной сетки — иначе сдвинутый для проверки
     * конец так и останется «ещё идущим», и призы не уйдут.
     * Пока текущий сезон не закончился, новый не создаётся.
     */
    @Transactional
    public JumpSeason catchUp(Instant now) {
        JumpSeason latest = seasons.lockLatest();
        while (latest.finishedAt(now)) {
            if (latest.awardedAt() == null) {
                award(latest);
                seasons.markAwarded(latest.id(), now);
                latest = latest.awarded(now);
            }
            latest = seasons.openNext(latest);
        }
        return latest;
    }

    private void award(JumpSeason season) {
        List<LeaderboardRow> top = seasons.top(season.id(), JumpSeasonCalendar.PRIZES.size());
        List<JumpSeasonPrizeLine> lines = new ArrayList<>(top.size());
        for (int i = 0; i < top.size(); i++) {
            LeaderboardRow row = top.get(i);
            int place = i + 1;
            long coins = JumpSeasonCalendar.PRIZES.get(i);
            String title = "Сезон " + season.seasonNumber() + " Bulba Jump: " + place + " место";
            credit.execute(row.playerId(), coins, BulbaCoinKind.JUMP_SEASON,
                    "jump-season:" + season.seasonNumber() + ":" + place, title);
            lines.add(new JumpSeasonPrizeLine(place, row.login(), row.value(), coins));
        }
        eventLog.jumpSeasonRewarded(season.seasonNumber(), lines);
    }
}
