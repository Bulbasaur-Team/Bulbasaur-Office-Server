package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bulbasaur.office.domain.model.Achievement;
import ru.bulbasaur.office.usecase.dto.LogEvent;
import ru.bulbasaur.office.usecase.port.out.EventLogPort;

import java.util.List;
import java.util.UUID;

/** Последние строки журнала событий — для «принтера логов» в дата-центре. */
@Service
@RequiredArgsConstructor
public class GetLogsUsecase {

    private static final int LIMIT = 500;

    private final EventLogPort log;
    private final AchievementService achievements;

    public List<LogEvent> execute(UUID playerId) {
        achievements.grant(playerId, Achievement.SYSADMIN);
        return log.recent(LIMIT);
    }
}
