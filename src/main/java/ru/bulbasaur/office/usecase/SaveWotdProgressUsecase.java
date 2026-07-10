package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bulbasaur.office.usecase.dto.WotdProgressView;
import ru.bulbasaur.office.usecase.dto.SaveWotdProgressCommand;
import ru.bulbasaur.office.usecase.dto.WotdProgressUpsert;
import ru.bulbasaur.office.usecase.port.out.WotdProgressRepositoryPort;
import ru.bulbasaur.office.usecase.port.out.DayPort;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SaveWotdProgressUsecase {

    private final WotdProgressRepositoryPort progress;
    private final DayPort day;
    private final AchievementService achievements;

    public WotdProgressView execute(SaveWotdProgressCommand command) {
        LocalDate today = day.today();
        Optional<WotdProgressView> existing = progress.findPlayerProgress(command.playerId(), command.game(), today);

        if (existing.map(WotdProgressView::solved).orElse(false)) {
            return existing.get();
        }

        // нельзя проставить меньше попыток, чем было
        int prevAttempts = existing.map(WotdProgressView::attempts).orElse(0);
        if (command.attempts() < prevAttempts) {
            return existing.get();
        }

        progress.upsertPlayerProgress(WotdProgressUpsert.builder()
                .playerId(command.playerId())
                .game(command.game())
                .day(today)
                .solved(command.solved())
                .attempts(command.attempts())
                .guesses(command.guesses())
                .build());
        achievements.recheck(command.playerId());
        return new WotdProgressView(command.solved(), command.attempts(), command.guesses());
    }
}
