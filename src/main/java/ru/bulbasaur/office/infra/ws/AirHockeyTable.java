package ru.bulbasaur.office.infra.ws;

import org.springframework.web.socket.WebSocketSession;
import ru.bulbasaur.office.infra.ws.dto.AirHockeyLobbyOut;
import ru.bulbasaur.office.infra.ws.dto.AirHockeyStateOut;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Стол аэрохоккея в одной локации. Ждёт двух игроков на разных сторонах, затем
 * крутит физику шайбы на сервере до 10 очков, таймаута или пока оба не уйдут.
 * Поле вертикальное: красный внизу, синий сверху.
 */
public class AirHockeyTable {

    public static final String LOCATION_ID = "chill-zone";
    public static final long MATCH_MS = 5 * 60 * 1000L;
    public static final int SCORE_TO_WIN = 10;

    /** Логический размер поля (как на клиенте): узкое × высокое. */
    public static final double W = 420;
    public static final double H = 700;

    private static final double PADDLE_R = 28;
    private static final double PUCK_R = 14;
    private static final double GOAL_HALF = 55;
    private static final double FRICTION = 0.995;
    private static final double MAX_PUCK_SPEED = 980;
    private static final double WALL_REST = 0.92;
    private static final double MIN_SERVE = 180;

    /** Сила удара от скорости биты (px/s), с полом и потолком. */
    private static final double PADDLE_SPEED_SCALE = 0.85;
    private static final double MIN_HIT_IMPULSE = 140;
    private static final double MAX_HIT_IMPULSE = 820;
    private static final double MAX_PADDLE_SPEED = 1600;
    private static final double PADDLE_VEL_TRANSFER = 0.3;
    private static final long PADDLE_VEL_STALE_MS = 80;

    public enum Phase {
        IDLE,
        WAITING,
        PLAYING,
        ENDED
    }

    public record Seat(
            UUID playerId,
            String login,
            WebSocketSession session,
            boolean connected
    ) {
        Seat withSession(WebSocketSession s) {
            return new Seat(playerId, login, s, true);
        }

        Seat disconnected() {
            return new Seat(playerId, login, session, false);
        }
    }

    /** Итог партии для журнала событий. */
    public record FinishedMatch(
            String redLogin,
            String blueLogin,
            int redScore,
            int blueScore,
            String winnerLogin
    ) {
    }

    private Phase phase = Phase.IDLE;
    private Seat red;
    private Seat blue;

    private double redX = W * 0.5;
    private double redY = H * 0.78;
    private double redVelX;
    private double redVelY;
    private long redPaddleAt;

    private double blueX = W * 0.5;
    private double blueY = H * 0.22;
    private double blueVelX;
    private double blueVelY;
    private long bluePaddleAt;

    private double puckX = W * 0.5;
    private double puckY = H * 0.5;
    private double puckVx;
    private double puckVy;

    private int redScore;
    private int blueScore;
    private long endsAtMillis;
    private String winnerSide;
    private String winnerLogin;
    private boolean logged;

    public synchronized Phase phase() {
        return phase;
    }

    public synchronized boolean isIdle() {
        return phase == Phase.IDLE;
    }

    public synchronized Seat red() {
        return red;
    }

    public synchronized Seat blue() {
        return blue;
    }

    public synchronized Seat seatOf(UUID playerId) {
        if (red != null && red.playerId().equals(playerId)) {
            return red;
        }
        if (blue != null && blue.playerId().equals(playerId)) {
            return blue;
        }
        return null;
    }

    public synchronized AirHockeySide sideOf(UUID playerId) {
        if (red != null && red.playerId().equals(playerId)) {
            return AirHockeySide.RED;
        }
        if (blue != null && blue.playerId().equals(playerId)) {
            return AirHockeySide.BLUE;
        }
        return null;
    }

    /**
     * Занять сторону. Если обе заняты — стартует партия.
     * @return сообщение об ошибке или null при успехе
     */
    public synchronized String join(AirHockeySide side, UUID playerId, String login, WebSocketSession session) {
        if (phase == Phase.ENDED) {
            resetAfterEnd();
        }
        if (phase == Phase.PLAYING) {
            return "Стол уже занят, подождите окончания игры.";
        }
        // Уже сидит на этой стороне — ок (реконнект).
        Seat current = side == AirHockeySide.RED ? red : blue;
        if (current != null && current.playerId().equals(playerId)) {
            if (side == AirHockeySide.RED) {
                red = current.withSession(session);
            } else {
                blue = current.withSession(session);
            }
            return null;
        }
        // Уже на другой стороне — пересаживаем.
        if (red != null && red.playerId().equals(playerId)) {
            red = null;
        }
        if (blue != null && blue.playerId().equals(playerId)) {
            blue = null;
        }
        Seat other = side == AirHockeySide.RED ? blue : red;
        if (other != null && other.playerId().equals(playerId)) {
            // уже сняли выше
        }
        if (current != null && !current.playerId().equals(playerId)) {
            return "Эта сторона уже занята.";
        }
        Seat seat = new Seat(playerId, login, session, true);
        if (side == AirHockeySide.RED) {
            red = seat;
        } else {
            blue = seat;
        }
        if (red != null && blue != null) {
            startMatch();
        } else {
            phase = Phase.WAITING;
        }
        return null;
    }

    /** Уйти со стола. Возвращает итог, если партия из-за этого завершилась. */
    public synchronized FinishedMatch leave(UUID playerId) {
        if (red != null && red.playerId().equals(playerId)) {
            if (phase == Phase.PLAYING) {
                red = red.disconnected();
                return maybeFinishIfEmpty();
            }
            red = null;
            refreshWaiting();
            return null;
        }
        if (blue != null && blue.playerId().equals(playerId)) {
            if (phase == Phase.PLAYING) {
                blue = blue.disconnected();
                return maybeFinishIfEmpty();
            }
            blue = null;
            refreshWaiting();
            return null;
        }
        return null;
    }

    /** Обрыв сокета: в лобби — убрать, в игре — пометить отключённым. */
    public synchronized FinishedMatch disconnect(UUID playerId) {
        return leave(playerId);
    }

    public synchronized void setPaddle(UUID playerId, double x, double y) {
        if (phase != Phase.PLAYING) {
            return;
        }
        AirHockeySide side = sideOf(playerId);
        if (side == null) {
            return;
        }
        Seat seat = side == AirHockeySide.RED ? red : blue;
        if (seat == null || !seat.connected()) {
            return;
        }
        // Клиент шлёт координаты ВИДА (своя половина всегда внизу экрана).
        // На всякий случай принимаем и абсолют: если синий прислал y уже в верхней
        // половине — не переворачиваем повторно (иначе бита залипает на центре).
        double absX = x;
        double absY = y;
        if (side == AirHockeySide.BLUE && y > H * 0.5) {
            absX = W - x;
            absY = H - y;
        }
        long now = System.currentTimeMillis();
        if (side == AirHockeySide.RED) {
            double nx = clamp(absX, PADDLE_R + 4, W - PADDLE_R - 4);
            double ny = clamp(absY, H * 0.5 + PADDLE_R + 2, H - PADDLE_R - 4);
            updatePaddleVel(true, nx, ny, now);
            redX = nx;
            redY = ny;
            redPaddleAt = now;
        } else {
            double nx = clamp(absX, PADDLE_R + 4, W - PADDLE_R - 4);
            double ny = clamp(absY, PADDLE_R + 4, H * 0.5 - PADDLE_R - 2);
            updatePaddleVel(false, nx, ny, now);
            blueX = nx;
            blueY = ny;
            bluePaddleAt = now;
        }
    }

    /** Шаг физики. Возвращает итог, если партия только что закончилась. */
    public synchronized FinishedMatch tick(double dt, long nowMillis) {
        if (phase != Phase.PLAYING) {
            return null;
        }
        if (nowMillis >= endsAtMillis) {
            return finish();
        }
        stalePaddleVel(nowMillis);
        integrate(dt);
        collidePaddle(true);
        collidePaddle(false);
        if (redScore >= SCORE_TO_WIN || blueScore >= SCORE_TO_WIN) {
            return finish();
        }
        return null;
    }

    /** Лобби для облачков: sessionId только пока ждём второго игрока. */
    public synchronized AirHockeyLobbyOut lobbyOut() {
        if (phase != Phase.WAITING) {
            return AirHockeyLobbyOut.of(null, null, null, null, phase.name().toLowerCase());
        }
        return AirHockeyLobbyOut.of(
                red != null ? red.session().getId() : null,
                red != null ? red.login() : null,
                blue != null ? blue.session().getId() : null,
                blue != null ? blue.login() : null,
                "waiting"
        );
    }

    public synchronized AirHockeyStateOut stateFor(UUID playerId, long nowMillis) {
        AirHockeySide mine = sideOf(playerId);
        long remaining = phase == Phase.PLAYING
                ? Math.max(0, endsAtMillis - nowMillis)
                : 0;
        // Координаты в виде получателя: своя бита внизу, чужая сверху.
        boolean flip = mine == AirHockeySide.BLUE;
        double viewPuckX = flip ? W - puckX : puckX;
        double viewPuckY = flip ? H - puckY : puckY;
        double viewPuckVx = flip ? -puckVx : puckVx;
        double viewPuckVy = flip ? -puckVy : puckVy;
        double myX;
        double myY;
        double oppX;
        double oppY;
        if (mine == AirHockeySide.BLUE) {
            myX = W - blueX;
            myY = H - blueY;
            oppX = W - redX;
            oppY = H - redY;
        } else {
            myX = redX;
            myY = redY;
            oppX = blueX;
            oppY = blueY;
        }
        return AirHockeyStateOut.of(
                phase.name().toLowerCase(),
                mine != null ? mine.code() : null,
                redScore,
                blueScore,
                remaining,
                viewPuckX, viewPuckY, viewPuckVx, viewPuckVy,
                myX, myY,
                oppX, oppY,
                red != null ? red.login() : null,
                blue != null ? blue.login() : null,
                red != null && red.connected(),
                blue != null && blue.connected(),
                winnerSide,
                winnerLogin
        );
    }

    public synchronized void resetAfterEnd() {
        if (phase == Phase.ENDED) {
            phase = Phase.IDLE;
            red = null;
            blue = null;
            winnerSide = null;
            winnerLogin = null;
            logged = false;
            redScore = 0;
            blueScore = 0;
            resetPuck(0);
        }
    }

    public synchronized boolean markLogged() {
        if (logged) {
            return false;
        }
        logged = true;
        return true;
    }

    private void startMatch() {
        phase = Phase.PLAYING;
        redScore = 0;
        blueScore = 0;
        winnerSide = null;
        winnerLogin = null;
        logged = false;
        endsAtMillis = System.currentTimeMillis() + MATCH_MS;
        redX = W * 0.5;
        redY = H * 0.78;
        redVelX = 0;
        redVelY = 0;
        redPaddleAt = System.currentTimeMillis();
        blueX = W * 0.5;
        blueY = H * 0.22;
        blueVelX = 0;
        blueVelY = 0;
        bluePaddleAt = redPaddleAt;
        resetPuck(ThreadLocalRandom.current().nextBoolean() ? -1 : 1);
    }

    private void refreshWaiting() {
        if (red == null && blue == null) {
            phase = Phase.IDLE;
        } else {
            phase = Phase.WAITING;
        }
    }

    private FinishedMatch maybeFinishIfEmpty() {
        boolean redAlive = red != null && red.connected();
        boolean blueAlive = blue != null && blue.connected();
        if (!redAlive && !blueAlive) {
            return finish();
        }
        return null;
    }

    private FinishedMatch finish() {
        if (phase == Phase.ENDED) {
            return new FinishedMatch(
                    red != null ? red.login() : "?",
                    blue != null ? blue.login() : "?",
                    redScore, blueScore, winnerLogin);
        }
        phase = Phase.ENDED;
        if (redScore > blueScore) {
            winnerSide = AirHockeySide.RED.code();
            winnerLogin = red != null ? red.login() : null;
        } else if (blueScore > redScore) {
            winnerSide = AirHockeySide.BLUE.code();
            winnerLogin = blue != null ? blue.login() : null;
        } else {
            winnerSide = null;
            winnerLogin = null;
        }
        puckVx = 0;
        puckVy = 0;
        return new FinishedMatch(
                red != null ? red.login() : "?",
                blue != null ? blue.login() : "?",
                redScore, blueScore, winnerLogin);
    }

    /** {@code dirY}: +1 — подача вниз (к красному), −1 — вверх (к синему). */
    private void resetPuck(int dirY) {
        puckX = W * 0.5;
        puckY = H * 0.5;
        double angle = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.7;
        double speed = MIN_SERVE;
        int dir = dirY == 0 ? (ThreadLocalRandom.current().nextBoolean() ? -1 : 1) : Integer.signum(dirY);
        puckVx = Math.sin(angle) * speed;
        puckVy = Math.cos(angle) * speed * dir;
    }

    private void integrate(double dt) {
        puckX += puckVx * dt;
        puckY += puckVy * dt;
        puckVx *= Math.pow(FRICTION, dt * 60);
        puckVy *= Math.pow(FRICTION, dt * 60);
        clampPuckSpeed();

        // Левый/правый борт.
        if (puckX < PUCK_R) {
            puckX = PUCK_R;
            puckVx = Math.abs(puckVx) * WALL_REST;
        } else if (puckX > W - PUCK_R) {
            puckX = W - PUCK_R;
            puckVx = -Math.abs(puckVx) * WALL_REST;
        }

        boolean inGoalX = Math.abs(puckX - W * 0.5) <= GOAL_HALF;

        // Верхний гол (ворота синих) — очко красным.
        if (puckY < PUCK_R) {
            if (inGoalX) {
                redScore++;
                resetPuck(1);
            } else {
                puckY = PUCK_R;
                puckVy = Math.abs(puckVy) * WALL_REST;
            }
        }
        // Нижний гол (ворота красных) — очко синим.
        if (puckY > H - PUCK_R) {
            if (inGoalX) {
                blueScore++;
                resetPuck(-1);
            } else {
                puckY = H - PUCK_R;
                puckVy = -Math.abs(puckVy) * WALL_REST;
            }
        }
    }

    private void collidePaddle(boolean isRed) {
        Seat seat = isRed ? red : blue;
        if (seat == null || !seat.connected()) {
            return;
        }
        double px = isRed ? redX : blueX;
        double py = isRed ? redY : blueY;
        double pvx = isRed ? redVelX : blueVelX;
        double pvy = isRed ? redVelY : blueVelY;

        double dx = puckX - px;
        double dy = puckY - py;
        double minDist = PADDLE_R + PUCK_R;
        double dist = Math.hypot(dx, dy);
        if (dist >= minDist || dist < 1e-6) {
            return;
        }
        double nx = dx / dist;
        double ny = dy / dist;
        puckX = px + nx * minDist;
        puckY = py + ny * minDist;

        double paddleSpeed = Math.hypot(pvx, pvy);
        double impulse = clamp(paddleSpeed * PADDLE_SPEED_SCALE, MIN_HIT_IMPULSE, MAX_HIT_IMPULSE);

        double relVx = puckVx - pvx;
        double relVy = puckVy - pvy;
        double vn = relVx * nx + relVy * ny;
        if (vn < 0) {
            puckVx -= (1.0 + WALL_REST) * vn * nx;
            puckVy -= (1.0 + WALL_REST) * vn * ny;
        }
        puckVx += pvx * PADDLE_VEL_TRANSFER;
        puckVy += pvy * PADDLE_VEL_TRANSFER;

        // Чем быстрее двигали биту — тем сильнее уходит шайба по нормали.
        double along = puckVx * nx + puckVy * ny;
        if (along < impulse) {
            puckVx += nx * (impulse - along);
            puckVy += ny * (impulse - along);
        }
        clampPuckSpeed();
    }

    private void updatePaddleVel(boolean isRed, double nx, double ny, long now) {
        long prevAt = isRed ? redPaddleAt : bluePaddleAt;
        double ox = isRed ? redX : blueX;
        double oy = isRed ? redY : blueY;
        double dtSec = (now - prevAt) / 1000.0;
        double vx = 0;
        double vy = 0;
        if (dtSec > 1e-4 && dtSec < 0.35) {
            vx = (nx - ox) / dtSec;
            vy = (ny - oy) / dtSec;
            double sp = Math.hypot(vx, vy);
            if (sp > MAX_PADDLE_SPEED) {
                double s = MAX_PADDLE_SPEED / sp;
                vx *= s;
                vy *= s;
            }
        }
        if (isRed) {
            redVelX = vx;
            redVelY = vy;
        } else {
            blueVelX = vx;
            blueVelY = vy;
        }
    }

    private void stalePaddleVel(long now) {
        if (now - redPaddleAt > PADDLE_VEL_STALE_MS) {
            redVelX = 0;
            redVelY = 0;
        }
        if (now - bluePaddleAt > PADDLE_VEL_STALE_MS) {
            blueVelX = 0;
            blueVelY = 0;
        }
    }

    private void clampPuckSpeed() {
        double sp = Math.hypot(puckVx, puckVy);
        if (sp > MAX_PUCK_SPEED) {
            double s = MAX_PUCK_SPEED / sp;
            puckVx *= s;
            puckVy *= s;
        }
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
