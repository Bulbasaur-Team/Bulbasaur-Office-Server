package ru.bulbasaur.office.infra.ws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ru.bulbasaur.office.infra.ws.dto.AirHockeyJoinMessage;
import ru.bulbasaur.office.infra.ws.dto.AirHockeyPaddleMessage;
import ru.bulbasaur.office.infra.ws.dto.AirHockeyRematchRespondMessage;
import ru.bulbasaur.office.infra.ws.dto.CatTalkMessage;
import ru.bulbasaur.office.infra.ws.dto.EmoteMessage;
import ru.bulbasaur.office.infra.ws.dto.ItemDropMessage;
import ru.bulbasaur.office.infra.ws.dto.ItemGoneMessage;
import ru.bulbasaur.office.infra.ws.dto.ItemGrabMessage;
import ru.bulbasaur.office.infra.ws.dto.ItemKickMessage;
import ru.bulbasaur.office.infra.ws.dto.ItemMoveMessage;
import ru.bulbasaur.office.infra.ws.dto.ItemPlaceMessage;
import ru.bulbasaur.office.infra.ws.dto.JoinMessage;
import ru.bulbasaur.office.infra.ws.dto.MoveMessage;
import ru.bulbasaur.office.infra.ws.dto.PokerCreateMessage;
import ru.bulbasaur.office.infra.ws.dto.PokerJoinMessage;
import ru.bulbasaur.office.infra.ws.dto.PokerTaskMessage;
import ru.bulbasaur.office.infra.ws.dto.PokerVoteMessage;
import ru.bulbasaur.office.infra.ws.dto.PongOut;
import ru.bulbasaur.office.infra.ws.dto.ProjectorIndexMessage;
import ru.bulbasaur.office.infra.ws.dto.ProjectorOnMessage;
import ru.bulbasaur.office.infra.ws.dto.RoomMessage;
import ru.bulbasaur.office.infra.ws.handler.AirHockeyWsHandler;
import ru.bulbasaur.office.infra.ws.handler.CatWsHandler;
import ru.bulbasaur.office.infra.ws.handler.ItemWsHandler;
import ru.bulbasaur.office.infra.ws.handler.PokerWsHandler;
import ru.bulbasaur.office.infra.ws.handler.PresenceLifecycle;
import ru.bulbasaur.office.infra.ws.handler.PresenceWsHandler;
import ru.bulbasaur.office.infra.ws.handler.ProjectorWsHandler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

/**
 * Тонкий роутер реалтайма: парсит JSON {@code type} и делегирует доменным хендлерам.
 * Присутствие — {@link PresenceRegistry}; рассылка — {@link WsMessenger}.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PresenceWebSocketHandler extends TextWebSocketHandler {

    private final PresenceRegistry registry;
    private final JsonMapper jsonMapper;
    private final WsMessenger messenger;
    private final PresenceLifecycle lifecycle;
    private final PresenceWsHandler presence;
    private final ItemWsHandler items;
    private final PokerWsHandler poker;
    private final ProjectorWsHandler projector;
    private final CatWsHandler cat;
    private final AirHockeyWsHandler airHockey;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        UUID playerId = (UUID) session.getAttributes().get(JwtHandshakeInterceptor.PLAYER_ID);
        String login = (String) session.getAttributes().get(JwtHandshakeInterceptor.LOGIN);
        registry.register(session, playerId, login);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String type = "?";
        try {
            JsonNode node = jsonMapper.readTree(message.getPayload());
            type = node.path("type").asString();
            switch (type) {
                case "ping" -> messenger.send(session, PongOut.of(node.path("t").asLong()));
                case "join" -> presence.onJoin(session, jsonMapper.treeToValue(node, JoinMessage.class));
                case "move" -> presence.onMove(session, jsonMapper.treeToValue(node, MoveMessage.class));
                case "room" -> presence.onRoom(session, jsonMapper.treeToValue(node, RoomMessage.class));
                case "emote" -> presence.onEmote(session, jsonMapper.treeToValue(node, EmoteMessage.class));
                case "itemKick" -> items.onKick(session, jsonMapper.treeToValue(node, ItemKickMessage.class));
                case "itemMove" -> items.onMove(session, jsonMapper.treeToValue(node, ItemMoveMessage.class));
                case "itemGrab" -> items.onGrab(session, jsonMapper.treeToValue(node, ItemGrabMessage.class));
                case "itemDrop" -> items.onDrop(session, jsonMapper.treeToValue(node, ItemDropMessage.class));
                case "itemPlace" -> items.onPlace(session, jsonMapper.treeToValue(node, ItemPlaceMessage.class));
                case "itemGone" -> items.onGone(session, jsonMapper.treeToValue(node, ItemGoneMessage.class));
                case "pokerList" -> poker.onList(session);
                case "pokerCreate" -> poker.onCreate(session, jsonMapper.treeToValue(node, PokerCreateMessage.class));
                case "pokerJoin" -> poker.onJoin(session, jsonMapper.treeToValue(node, PokerJoinMessage.class));
                case "pokerLeave" -> poker.onLeave(session);
                case "pokerAddTask" -> poker.onAddTask(session, jsonMapper.treeToValue(node, PokerTaskMessage.class));
                case "pokerVote" -> poker.onVote(session, jsonMapper.treeToValue(node, PokerVoteMessage.class));
                case "pokerFinish" -> poker.onFinish(session);
                case "pokerClose" -> poker.onClose(session);
                case "projectorOn" -> projector.onOn(session, jsonMapper.treeToValue(node, ProjectorOnMessage.class));
                case "projectorOff" -> projector.onOff(session);
                case "projectorIndex" -> projector.onIndex(session, jsonMapper.treeToValue(node, ProjectorIndexMessage.class));
                case "catTalk" -> cat.onTalk(session, jsonMapper.treeToValue(node, CatTalkMessage.class));
                case "catAdvice" -> cat.onAdvice(session);
                case "airhockeyJoin" -> airHockey.onJoin(session, jsonMapper.treeToValue(node, AirHockeyJoinMessage.class));
                case "airhockeyLeave" -> airHockey.onLeave(session);
                case "airhockeyPaddle" -> airHockey.onPaddle(session, jsonMapper.treeToValue(node, AirHockeyPaddleMessage.class));
                case "airhockeyRematchRequest" -> airHockey.onRematchRequest(session);
                case "airhockeyRematchCancel" -> airHockey.onRematchCancel(session);
                case "airhockeyRematchRespond" -> airHockey.onRematchRespond(
                        session, jsonMapper.treeToValue(node, AirHockeyRematchRespondMessage.class));
                // "chat" — чат временно отключён: сообщения не обрабатываются и не рассылаются.
                default -> log.debug("неизвестный/отключённый тип WS-сообщения: {}", type);
            }
        } catch (Exception e) {
            log.error("ошибка обработки WS-сообщения type={}", type, e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        lifecycle.onDisconnect(session);
    }
}
