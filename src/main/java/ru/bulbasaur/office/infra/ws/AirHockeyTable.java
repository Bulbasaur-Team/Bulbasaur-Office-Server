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
    public static final long MATCH_MS = 3 * 60 * 1000L;
    public static final int SCORE_TO_WIN = 10;

    /** Логический размер поля (как на клиенте): узкое × высокое. */
    public static final double W = 420;
    public static final double H = 700;

    private static final double PADDLE_R = 34.2; // 29.75 × 1.15
    private static final double PUCK_R = 22.75; // 18.2 × 1.25
    /**
     * Хитбокс чуть меньше логических радиусов: в спрайтах есть прозрачные края,
     * иначе удар срабатывает до видимого касания.
     */
    private static final double CONTACT_SCALE = 0.9;
    private static final double GOAL_HALF = 55;
    private static final double FRICTION = 0.995;
    private static final double MAX_PUCK_SPEED = 1904; // 1656 × 1.15
    private static final double WALL_REST = 0.92;
    private static final double MIN_SERVE = 180;
    private static final long GOAL_FREEZE_MS = 1000L;
    private static final long SERVE_RAMP_MS = 900L;
    /** Стартовая доля целевой скорости при мягком вбросе. */
    private static final double SERVE_START_FRAC = 0.12;

    /**
     * Упругость удара бита–шайба. Импульс считается из относительной скорости
     * по нормали, поэтому сила удара растёт со скоростью замаха сама собой,
     * а повторное срабатывание в одном контакте физически невозможно
     * (после отражения тела уже расходятся).
     */
    private static final double HIT_RESTITUTION = 0.88;
    private static final double MAX_PADDLE_SPEED = 1600;
    private static final long PADDLE_VEL_STALE_MS = 80;
    /** Шаг прохода биты при CCD — заметно меньше радиуса контакта. */
    private static final double PADDLE_SWEEP_STEP = 4;

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
    /** Сторона, предложившая реванш ({@code red}/{@code blue}), или null. */
    private String rematchBy;

    private long goalFreezeUntil;
    private String goalScorerLogin;
    private int pendingServeDir;
    private boolean awaitingServeAfterGoal;
    private long serveRampUntil;
    private double serveTargetVx;
    private double serveTargetVy;

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
            if (hasConnectedSeat()) {
                return "Стол занят — игроки ещё за столом.";
            }
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
            rematchBy = null;
            red = null;
            if (phase == Phase.ENDED) {
                if (!hasConnectedSeat()) {
                    clearToIdle();
                }
                return null;
            }
            refreshWaiting();
            return null;
        }
        if (blue != null && blue.playerId().equals(playerId)) {
            if (phase == Phase.PLAYING) {
                blue = blue.disconnected();
                return maybeFinishIfEmpty();
            }
            rematchBy = null;
            blue = null;
            if (phase == Phase.ENDED) {
                if (!hasConnectedSeat()) {
                    clearToIdle();
                }
                return null;
            }
            refreshWaiting();
            return null;
        }
        return null;
    }

    public synchronized boolean hasConnectedSeat() {
        return (red != null && red.connected()) || (blue != null && blue.connected());
    }

    /**
     * Предложить реванш. {@code null} — ок, иначе текст ошибки.
     */
    public synchronized String requestRematch(UUID playerId) {
        if (phase != Phase.ENDED) {
            return "Реванш доступен только после партии.";
        }
        AirHockeySide side = sideOf(playerId);
        if (side == null) {
            return "Ты не за столом.";
        }
        Seat mine = side == AirHockeySide.RED ? red : blue;
        Seat opp = side == AirHockeySide.RED ? blue : red;
        if (mine == null || !mine.connected() || opp == null || !opp.connected()) {
            return "Соперник уже ушёл.";
        }
        if (rematchBy != null) {
            return rematchBy.equals(side.code())
                    ? "Ожидай ответа соперника."
                    : "Соперник уже предложил реванш.";
        }
        rematchBy = side.code();
        return null;
    }

    /** Отменить своё предложение реванша. */
    public synchronized String cancelRematch(UUID playerId) {
        if (phase != Phase.ENDED) {
            return "Сейчас нельзя отменить реванш.";
        }
        AirHockeySide side = sideOf(playerId);
        if (side == null || rematchBy == null || !rematchBy.equals(side.code())) {
            return "Нет активного предложения.";
        }
        rematchBy = null;
        return null;
    }

    /**
     * Ответ на реванш. При accept=true стартует новая партия.
     * {@code null} — ок, иначе ошибка.
     */
    public synchronized String respondRematch(UUID playerId, boolean accept) {
        if (phase != Phase.ENDED) {
            return "Реванш недоступен.";
        }
        AirHockeySide side = sideOf(playerId);
        if (side == null || rematchBy == null) {
            return "Нет предложения реванша.";
        }
        if (rematchBy.equals(side.code())) {
            return "Нельзя ответить на своё предложение.";
        }
        Seat mine = side == AirHockeySide.RED ? red : blue;
        Seat opp = side == AirHockeySide.RED ? blue : red;
        if (mine == null || !mine.connected() || opp == null || !opp.connected()) {
            rematchBy = null;
            return "Соперник уже ушёл.";
        }
        if (!accept) {
            rematchBy = null;
            return null;
        }
        rematchBy = null;
        startMatch();
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
        boolean freeze = now < goalFreezeUntil;
        if (side == AirHockeySide.RED) {
            double nx = clamp(absX, PADDLE_R + 4, W - PADDLE_R - 4);
            double ny = clamp(absY, H * 0.5 + PADDLE_R + 2, H - PADDLE_R - 4);
            if (freeze) {
                updatePaddleVel(true, nx, ny, now);
                redX = nx;
                redY = ny;
                redPaddleAt = now;
            } else {
                sweepPaddle(true, nx, ny, now);
            }
        } else {
            double nx = clamp(absX, PADDLE_R + 4, W - PADDLE_R - 4);
            double ny = clamp(absY, PADDLE_R + 4, H * 0.5 - PADDLE_R - 2);
            if (freeze) {
                updatePaddleVel(false, nx, ny, now);
                blueX = nx;
                blueY = ny;
                bluePaddleAt = now;
            } else {
                sweepPaddle(false, nx, ny, now);
            }
        }
    }

    /**
     * Двигает биту от старой позиции к новой с непрерывной коллизией (TOI + дожим).
     * Иначе быстрый жест телепортирует биту сквозь шайбу между сообщениями.
     */
    private void sweepPaddle(boolean isRed, double nx, double ny, long now) {
        double ox = isRed ? redX : blueX;
        double oy = isRed ? redY : blueY;
        updatePaddleVel(isRed, nx, ny, now);

        double minDist = (PADDLE_R + PUCK_R) * CONTACT_SCALE;
        // Уже внутри в стартовой точке.
        if (isRed) {
            redX = ox;
            redY = oy;
        } else {
            blueX = ox;
            blueY = oy;
        }
        collidePaddle(isRed);

        double dx = nx - ox;
        double dy = ny - oy;
        double travel = Math.hypot(dx, dy);
        if (travel < 1e-6) {
            if (isRed) {
                redPaddleAt = now;
            } else {
                bluePaddleAt = now;
            }
            return;
        }

        // Непрерывный удар: ставим биту в первый момент касания по траектории.
        double tHit = sweptCircleT(ox, oy, nx, ny, puckX, puckY, minDist);
        double fromX = ox;
        double fromY = oy;
        if (tHit >= 0 && tHit <= 1) {
            fromX = ox + dx * tHit;
            fromY = oy + dy * tHit;
            if (isRed) {
                redX = fromX;
                redY = fromY;
            } else {
                blueX = fromX;
                blueY = fromY;
            }
            collidePaddle(isRed);
        }

        // Остаток пути мелкими шагами: физический импульс сработает максимум
        // один раз (пока vn < 0), дальше — только позиционное разделение.
        double remain = Math.hypot(nx - fromX, ny - fromY);
        int steps = Math.max(1, (int) Math.ceil(remain / PADDLE_SWEEP_STEP));
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            if (isRed) {
                redX = fromX + (nx - fromX) * t;
                redY = fromY + (ny - fromY) * t;
            } else {
                blueX = fromX + (nx - fromX) * t;
                blueY = fromY + (ny - fromY) * t;
            }
            collidePaddle(isRed);
        }
        if (isRed) {
            redX = nx;
            redY = ny;
            redPaddleAt = now;
        } else {
            blueX = nx;
            blueY = ny;
            bluePaddleAt = now;
        }
        collidePaddle(isRed);
    }

    /**
     * Время первого касания движущегося круга (A→B) со статичной точкой радиуса r.
     * {@code -1}, если касания на отрезке нет.
     */
    private static double sweptCircleT(
            double ax, double ay, double bx, double by,
            double px, double py, double r
    ) {
        double dx = bx - ax;
        double dy = by - ay;
        double fx = ax - px;
        double fy = ay - py;
        double a = dx * dx + dy * dy;
        double b = 2 * (fx * dx + fy * dy);
        double c = fx * fx + fy * fy - r * r;
        if (c <= 0) {
            return 0; // уже пересекаемся в A
        }
        if (a < 1e-12) {
            return -1;
        }
        double disc = b * b - 4 * a * c;
        if (disc < 0) {
            return -1;
        }
        double t = (-b - Math.sqrt(disc)) / (2 * a);
        if (t < 0 || t > 1) {
            return -1;
        }
        return t;
    }

    /** Шаг физики. Возвращает итог, если партия только что закончилась. */
    public synchronized FinishedMatch tick(double dt, long nowMillis) {
        if (phase != Phase.PLAYING) {
            return null;
        }
        if (nowMillis >= endsAtMillis) {
            return finish();
        }
        // Пауза после гола: шайба в центре, надпись у клиентов.
        if (nowMillis < goalFreezeUntil) {
            puckX = W * 0.5;
            puckY = H * 0.5;
            puckVx = 0;
            puckVy = 0;
            return null;
        }
        if (awaitingServeAfterGoal) {
            awaitingServeAfterGoal = false;
            goalScorerLogin = null;
            goalFreezeUntil = 0;
            if (redScore >= SCORE_TO_WIN || blueScore >= SCORE_TO_WIN) {
                return finish();
            }
            beginSoftServe(pendingServeDir, nowMillis);
        }
        stalePaddleVel(nowMillis);
        rampServe(nowMillis);
        // Быстрая шайба тоже может проскочить биту за один dt — режем шаг.
        double speed = Math.hypot(puckVx, puckVy);
        int substeps = Math.max(1, (int) Math.ceil(speed * dt / PADDLE_SWEEP_STEP));
        double subDt = dt / substeps;
        for (int i = 0; i < substeps; i++) {
            integrate(subDt, nowMillis);
            if (awaitingServeAfterGoal || nowMillis < goalFreezeUntil) {
                break;
            }
            collidePaddle(true);
            collidePaddle(false);
        }
        if (!awaitingServeAfterGoal
                && (redScore >= SCORE_TO_WIN || blueScore >= SCORE_TO_WIN)) {
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
                winnerLogin,
                rematchBy,
                phase == Phase.PLAYING ? Math.max(0, goalFreezeUntil - nowMillis) : 0,
                goalScorerLogin
        );
    }

    public synchronized void resetAfterEnd() {
        if (phase == Phase.ENDED) {
            clearToIdle();
        }
    }

    private void clearToIdle() {
        phase = Phase.IDLE;
        red = null;
        blue = null;
        winnerSide = null;
        winnerLogin = null;
        rematchBy = null;
        logged = false;
        redScore = 0;
        blueScore = 0;
        goalFreezeUntil = 0;
        goalScorerLogin = null;
        serveRampUntil = 0;
        awaitingServeAfterGoal = false;
        resetPuckIdle();
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
        rematchBy = null;
        logged = false;
        goalFreezeUntil = 0;
        goalScorerLogin = null;
        serveRampUntil = 0;
        awaitingServeAfterGoal = false;
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
        beginSoftServe(ThreadLocalRandom.current().nextBoolean() ? -1 : 1, System.currentTimeMillis());
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
        rematchBy = null;
        goalFreezeUntil = 0;
        goalScorerLogin = null;
        serveRampUntil = 0;
        awaitingServeAfterGoal = false;
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

    /** Шайба в центре без скорости (пауза / idle). */
    private void resetPuckIdle() {
        puckX = W * 0.5;
        puckY = H * 0.5;
        puckVx = 0;
        puckVy = 0;
        serveRampUntil = 0;
    }

    /**
     * Мягкий вброс: шайба стартует медленно и за {@link #SERVE_RAMP_MS} разгоняется
     * до целевой скорости — без резкого «выстрела» из центра.
     */
    private void beginSoftServe(int dirY, long nowMillis) {
        puckX = W * 0.5;
        puckY = H * 0.5;
        double angle = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.7;
        double speed = MIN_SERVE;
        int dir = dirY == 0 ? (ThreadLocalRandom.current().nextBoolean() ? -1 : 1) : Integer.signum(dirY);
        serveTargetVx = Math.sin(angle) * speed;
        serveTargetVy = Math.cos(angle) * speed * dir;
        puckVx = serveTargetVx * SERVE_START_FRAC;
        puckVy = serveTargetVy * SERVE_START_FRAC;
        serveRampUntil = nowMillis + SERVE_RAMP_MS;
    }

    private void rampServe(long nowMillis) {
        if (serveRampUntil <= 0 || nowMillis >= serveRampUntil) {
            if (serveRampUntil > 0) {
                puckVx = serveTargetVx;
                puckVy = serveTargetVy;
                serveRampUntil = 0;
            }
            return;
        }
        double t = 1.0 - (serveRampUntil - nowMillis) / (double) SERVE_RAMP_MS;
        t = clamp(t, 0, 1);
        // smoothstep
        t = t * t * (3 - 2 * t);
        double frac = SERVE_START_FRAC + (1.0 - SERVE_START_FRAC) * t;
        puckVx = serveTargetVx * frac;
        puckVy = serveTargetVy * frac;
    }

    /** Гол: пауза 1 с, шайба в центре, затем мягкий вброс в сторону пропустившего. */
    private void onGoal(boolean redScored, long nowMillis) {
        if (redScored) {
            redScore++;
            goalScorerLogin = red != null ? red.login() : "красный";
            pendingServeDir = 1; // вброс вниз (к красному), как раньше
        } else {
            blueScore++;
            goalScorerLogin = blue != null ? blue.login() : "синий";
            pendingServeDir = -1;
        }
        resetPuckIdle();
        goalFreezeUntil = nowMillis + GOAL_FREEZE_MS;
        awaitingServeAfterGoal = true;
        // Пауза не съедает время матча.
        endsAtMillis += GOAL_FREEZE_MS;
    }

    private void integrate(double dt, long nowMillis) {
        if (nowMillis < goalFreezeUntil) {
            return;
        }
        puckX += puckVx * dt;
        puckY += puckVy * dt;
        // Во время разгона вброса не трем скорость — иначе ramp не дотягивает.
        if (serveRampUntil <= 0 || nowMillis >= serveRampUntil) {
            puckVx *= Math.pow(FRICTION, dt * 60);
            puckVy *= Math.pow(FRICTION, dt * 60);
        }
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
                onGoal(true, nowMillis);
            } else {
                puckY = PUCK_R;
                puckVy = Math.abs(puckVy) * WALL_REST;
            }
        }
        // Нижний гол (ворота красных) — очко синим.
        if (puckY > H - PUCK_R) {
            if (inGoalX) {
                onGoal(false, nowMillis);
            } else {
                puckY = H - PUCK_R;
                puckVy = -Math.abs(puckVy) * WALL_REST;
            }
        }
    }

    /**
     * Коллизия бита–шайба: позиционное разделение + один физический импульс.
     * Импульс возникает только при сближении по нормали (vn < 0); сразу после
     * отражения тела расходятся, поэтому в одном контакте удар не повторится —
     * без «защёлок» и добивок, из-за которых шайба липла к бите.
     */
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
        double minDist = (PADDLE_R + PUCK_R) * CONTACT_SCALE;
        double dist = Math.hypot(dx, dy);
        if (dist >= minDist) {
            return;
        }

        double nx;
        double ny;
        if (dist < 1e-6) {
            nx = 0;
            ny = py < H * 0.5 ? 1 : -1;
        } else {
            nx = dx / dist;
            ny = dy / dist;
        }

        // Раздвигаем тела, чтобы не тонули друг в друге.
        puckX = px + nx * minDist;
        puckY = py + ny * minDist;
        puckX = clamp(puckX, PUCK_R, W - PUCK_R);
        puckY = clamp(puckY, PUCK_R, H - PUCK_R);

        // Скорость сближения по нормали (бита — «бесконечная масса»).
        double vn = (puckVx - pvx) * nx + (puckVy - pvy) * ny;
        if (vn >= 0) {
            return; // уже расходимся — удар в этом контакте состоялся
        }
        double j = -(1.0 + HIT_RESTITUTION) * vn;
        puckVx += j * nx;
        puckVy += j * ny;
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
