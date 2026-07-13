package ru.bulbasaur.office.infra.ws;

import ru.bulbasaur.office.infra.ws.dto.ItemStateDto;

/**
 * Состояние одного физичного предмета: позиция, скорость и владелец — сессия,
 * чей удар был принят последним; только её репорты позиции применяются. Удары
 * конкурентны (несколько игроков могут ударить одновременно), поэтому все
 * изменения — под монитором объекта: побеждает удар, попавший первым.
 */
public class ItemState {

    /** Окно арбитража: удары по уже ударенному предмету в этом окне отбрасываются. */
    private static final long KICK_WINDOW_MS = 250;

    private final String id;

    private double x;
    private double y;
    private double vx;
    private double vy;
    private String ownerSessionId;
    private long lastKickAtMillis;

    public ItemState(String id) {
        this.id = id;
    }

    /** Принять удар, если окно арбитража свободно. Принятый удар делает сессию владельцем. */
    public synchronized boolean tryKick(String sessionId, long nowMillis,
                                        double x, double y, double vx, double vy) {
        if (nowMillis - lastKickAtMillis < KICK_WINDOW_MS) {
            return false;
        }
        lastKickAtMillis = nowMillis;
        ownerSessionId = sessionId;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        return true;
    }

    /** Применить репорт позиции, если он от владельца (проигравший арбитраж мог ещё стримить). */
    public synchronized boolean moveByOwner(String sessionId, double x, double y, double vx, double vy) {
        if (!sessionId.equals(ownerSessionId)) {
            return false;
        }
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        return true;
    }

    /**
     * Предмет положили на землю: он замирает в этой точке и остаётся ничьим —
     * следующий удар любого игрока разыгрывается заново, без окна арбитража.
     */
    public synchronized void rest(double x, double y) {
        this.x = x;
        this.y = y;
        this.vx = 0;
        this.vy = 0;
        this.ownerSessionId = null;
        this.lastKickAtMillis = 0;
    }

    public synchronized ItemStateDto toDto() {
        return new ItemStateDto(id, x, y, vx, vy);
    }
}
