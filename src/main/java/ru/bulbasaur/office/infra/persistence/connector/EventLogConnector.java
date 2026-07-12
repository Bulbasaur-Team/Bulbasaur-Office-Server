package ru.bulbasaur.office.infra.persistence.connector;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.infra.persistence.entity.EventLogEntity;
import ru.bulbasaur.office.infra.persistence.repository.EventLogJpaRepository;
import ru.bulbasaur.office.usecase.dto.LogEvent;
import ru.bulbasaur.office.usecase.port.out.EventLogPort;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EventLogConnector implements EventLogPort {

    private final EventLogJpaRepository repository;

    @Override
    @Transactional
    public void append(String level, String logger, String message) {
        EventLogEntity entity = new EventLogEntity();
        entity.setId(UUID.randomUUID());
        entity.setCreatedAt(Instant.now());
        entity.setLevel(level);
        entity.setLogger(logger);
        entity.setMessage(message);
        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LogEvent> recent(int limit) {
        List<EventLogEntity> rows = repository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit));
        List<LogEvent> events = new ArrayList<>(rows.size());
        // В БД взяли последние по убыванию времени — для вывода разворачиваем к возрастанию.
        for (int i = rows.size() - 1; i >= 0; i--) {
            EventLogEntity row = rows.get(i);
            events.add(new LogEvent(row.getCreatedAt(), row.getLevel(), row.getLogger(), row.getMessage()));
        }
        return events;
    }
}
