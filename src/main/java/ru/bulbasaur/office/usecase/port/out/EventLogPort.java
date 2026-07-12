package ru.bulbasaur.office.usecase.port.out;

import ru.bulbasaur.office.usecase.dto.LogEvent;

import java.util.List;

/** Журнал событий: запись новой строки и чтение последних N (по возрастанию времени). */
public interface EventLogPort {

    void append(String level, String logger, String message);

    List<LogEvent> recent(int limit);
}
