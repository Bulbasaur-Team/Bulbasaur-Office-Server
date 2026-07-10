package ru.bulbasaur.office.infra.ws;

import tools.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import ru.bulbasaur.office.domain.model.Achievement;
import ru.bulbasaur.office.infra.ws.dto.AchievementOut;
import ru.bulbasaur.office.usecase.port.out.AchievementNotifierPort;

import java.io.IOException;
import java.util.UUID;

/** Слёт попапа о новой ачивке во все онлайн-сессии игрока. Оффлайн — просто ничего не шлём. */
@Component
@Slf4j
@RequiredArgsConstructor
public class AchievementNotifier implements AchievementNotifierPort {

    private final PresenceRegistry registry;
    private final JsonMapper jsonMapper;

    @Override
    public void notifyGranted(UUID playerId, Achievement achievement) {
        String payload = jsonMapper.writeValueAsString(AchievementOut.of(achievement));
        for (WebSocketSession session : registry.sessionsOf(playerId)) {
            if (!session.isOpen()) {
                continue;
            }
            try {
                synchronized (session) {
                    session.sendMessage(new TextMessage(payload));
                }
            } catch (IOException e) {
                log.debug("не удалось отправить попап ачивки сессии {}: {}", session.getId(), e.getMessage());
            }
        }
    }
}
