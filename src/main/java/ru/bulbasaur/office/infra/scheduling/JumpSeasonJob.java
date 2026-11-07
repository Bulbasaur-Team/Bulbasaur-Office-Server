package ru.bulbasaur.office.infra.scheduling;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.bulbasaur.office.usecase.JumpSeasonUsecase;

import java.time.Instant;

/**
 * Закрывает сезон Bulba Jump, когда вышло его окно: призы, запись в журнал и следующий сезон.
 * Тот же догон делается при входе в таблицу и при отправке результата, если джоба ещё не успела.
 */
@Component
@RequiredArgsConstructor
public class JumpSeasonJob {

    private final JumpSeasonUsecase seasons;

    @Scheduled(cron = "0 * * * * *")
    public void catchUp() {
        seasons.catchUp(Instant.now());
    }
}
