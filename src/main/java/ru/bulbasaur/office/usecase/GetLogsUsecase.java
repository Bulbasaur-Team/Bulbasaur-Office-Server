package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bulbasaur.office.usecase.dto.LogEvent;
import ru.bulbasaur.office.usecase.port.out.EventLogPort;

import java.util.List;

/** Последние строки журнала событий — для «принтера логов» в дата-центре. */
@Service
@RequiredArgsConstructor
public class GetLogsUsecase {

    private static final int LIMIT = 500;

    private final EventLogPort log;

    public List<LogEvent> execute() {
        return log.recent(LIMIT);
    }
}
