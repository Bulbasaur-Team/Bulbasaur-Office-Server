package ru.bulbasaur.office.infra.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.bulbasaur.office.infra.rest.dto.LogsResponse;
import ru.bulbasaur.office.infra.security.AuthPrincipal;
import ru.bulbasaur.office.usecase.GetLogsUsecase;
import ru.bulbasaur.office.usecase.dto.LogEvent;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.of("Europe/Moscow"));

    private final GetLogsUsecase getLogs;

    @GetMapping
    public LogsResponse logs(@AuthenticationPrincipal AuthPrincipal player) {
        List<String> lines = getLogs.execute(player.id()).stream().map(LogController::format).toList();
        return new LogsResponse(lines);
    }

    private static String format(LogEvent e) {
        return String.format("%s  %-5s  %s", TIMESTAMP.format(e.time()), e.level(), e.message());
    }
}
