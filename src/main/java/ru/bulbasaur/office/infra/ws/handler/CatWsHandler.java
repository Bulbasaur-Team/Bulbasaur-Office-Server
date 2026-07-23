package ru.bulbasaur.office.infra.ws.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import ru.bulbasaur.office.domain.model.Achievement;
import ru.bulbasaur.office.infra.ws.BulbaCatRegistry;
import ru.bulbasaur.office.infra.ws.PresenceRegistry;
import ru.bulbasaur.office.infra.ws.PresenceState;
import ru.bulbasaur.office.infra.ws.WsMessenger;
import ru.bulbasaur.office.infra.ws.dto.CatSayOut;
import ru.bulbasaur.office.infra.ws.dto.CatStateOut;
import ru.bulbasaur.office.infra.ws.dto.CatTalkMessage;
import ru.bulbasaur.office.usecase.AchievementService;
import ru.bulbasaur.office.usecase.CatAdviceService;

/** Бульба Кот: диалог (пауза), советы и серверный тик маршрута. */
@Component
@RequiredArgsConstructor
@Slf4j
public class CatWsHandler {

    private final PresenceRegistry registry;
    private final BulbaCatRegistry bulbaCatRegistry;
    private final AchievementService achievements;
    private final CatAdviceService catAdvice;
    private final WsMessenger messenger;

    public void onTalk(WebSocketSession session, CatTalkMessage msg) {
        PresenceState state = registry.get(session.getId());
        if (state == null || !state.isPlaced()) {
            return;
        }
        if (!BulbaCatRegistry.LOCATION_ID.equals(state.locationId())) {
            return;
        }
        if (msg.talking()) {
            achievements.grant(state.playerId(), Achievement.MEOW);
        }
        CatStateOut out = bulbaCatRegistry.setTalking(session.getId(), msg.talking());
        if (out != null) {
            messenger.broadcastAll(BulbaCatRegistry.LOCATION_ID, out);
        }
    }

    public void onAdvice(WebSocketSession session) {
        PresenceState state = registry.get(session.getId());
        if (state == null || !state.isPlaced()) {
            log.debug("catAdvice: игрок не в мире, session={}", session.getId());
            return;
        }
        if (!BulbaCatRegistry.LOCATION_ID.equals(state.locationId())) {
            log.debug("catAdvice: не main-office ({}), login={}", state.locationId(), state.login());
            return;
        }
        String text;
        try {
            text = catAdvice.pickAdvice(state.playerId());
        } catch (Exception e) {
            log.error("не удалось подобрать совет кота для {}", state.login(), e);
            text = "Что-то пошло не так… мяукни ещё раз чуть позже.";
        }
        messenger.send(session, CatSayOut.of(text));
    }

    /** Снять диалог сессии (disconnect / уход из main-office). */
    public void clearTalkingAndBroadcast(String sessionId) {
        CatStateOut cat = bulbaCatRegistry.clearTalking(sessionId);
        if (cat != null) {
            messenger.broadcastAll(BulbaCatRegistry.LOCATION_ID, cat);
        }
    }

    @Scheduled(fixedRate = 100)
    public void tickBulbaCat() {
        CatStateOut state = bulbaCatRegistry.tick(0.1, System.currentTimeMillis());
        if (state != null) {
            messenger.broadcastAll(BulbaCatRegistry.LOCATION_ID, state);
        }
    }
}
