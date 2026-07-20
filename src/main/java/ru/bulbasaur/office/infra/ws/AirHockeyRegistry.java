package ru.bulbasaur.office.infra.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import ru.bulbasaur.office.infra.ws.dto.AirHockeyLobbyOut;
import ru.bulbasaur.office.infra.ws.dto.AirHockeyLobbyOut.WaitingSeat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Реестр столов аэрохоккея в чилл-зоне. Каждый матч — отдельный стол;
 * при join ищем WAITING-стол, которому нужна сторона, иначе создаём новый.
 */
@Component
public class AirHockeyRegistry {

    private final Map<String, AirHockeyTable> tables = new ConcurrentHashMap<>();

    /** Стол, за которым сидит игрок, или null. */
    public AirHockeyTable tableOf(UUID playerId) {
        for (AirHockeyTable table : tables.values()) {
            if (table.seatOf(playerId) != null) {
                return table;
            }
        }
        return null;
    }

    /** Все активные столы (для тика физики). */
    public Collection<AirHockeyTable> all() {
        return List.copyOf(tables.values());
    }

    /**
     * Занять сторону: реконнект / реванш — на своём столе; иначе матчмейкинг.
     * @return стол или null, если join вернул ошибку (тогда {@code errorOut[0]} заполнен)
     */
    public AirHockeyTable join(
            AirHockeySide side,
            UUID playerId,
            String login,
            WebSocketSession session,
            String[] errorOut
    ) {
        AirHockeyTable existing = tableOf(playerId);
        if (existing != null) {
            AirHockeyTable.Phase phase = existing.phase();
            // В партии или на экране реванша — остаёмся за тем же столом.
            if (phase == AirHockeyTable.Phase.PLAYING || phase == AirHockeyTable.Phase.ENDED) {
                String error = existing.join(side, playerId, login, session);
                if (error != null) {
                    errorOut[0] = error;
                    return null;
                }
                return existing;
            }
            // WAITING: уходим и матчим заново (смена стороны / повторный join).
            existing.leave(playerId);
            removeIfIdle(existing);
        }

        // Несколько попыток: два игрока могут одновременно сесть на одну сторону.
        for (int attempt = 0; attempt < 4; attempt++) {
            AirHockeyTable open = findWaitingFor(side);
            if (open == null) {
                open = new AirHockeyTable(UUID.randomUUID().toString());
                tables.put(open.id(), open);
            }
            String error = open.join(side, playerId, login, session);
            if (error == null) {
                return open;
            }
            removeIfIdle(open);
            // Гонка за свободную сторону — пробуем другой / новый стол.
            if ("Эта сторона уже занята.".equals(error)) {
                continue;
            }
            errorOut[0] = error;
            return null;
        }
        errorOut[0] = "Не удалось сесть за стол, попробуй ещё раз.";
        return null;
    }

    /** Убрать стол, если пустой. */
    public void removeIfIdle(AirHockeyTable table) {
        if (table != null && table.isIdle()) {
            tables.remove(table.id(), table);
        }
    }

    /** Сводное лобби: все, кто ждёт соперника на любом столе. */
    public AirHockeyLobbyOut lobbyOut() {
        List<WaitingSeat> waiting = new ArrayList<>();
        for (AirHockeyTable table : tables.values()) {
            if (table.phase() != AirHockeyTable.Phase.WAITING) {
                continue;
            }
            AirHockeyTable.Seat red = table.red();
            if (red != null && red.connected()) {
                waiting.add(new WaitingSeat(AirHockeySide.RED.code(), red.session().getId(), red.login()));
            }
            AirHockeyTable.Seat blue = table.blue();
            if (blue != null && blue.connected()) {
                waiting.add(new WaitingSeat(AirHockeySide.BLUE.code(), blue.session().getId(), blue.login()));
            }
        }
        return AirHockeyLobbyOut.of(waiting);
    }

    private AirHockeyTable findWaitingFor(AirHockeySide side) {
        for (AirHockeyTable table : tables.values()) {
            if (table.needs(side)) {
                return table;
            }
        }
        return null;
    }
}
