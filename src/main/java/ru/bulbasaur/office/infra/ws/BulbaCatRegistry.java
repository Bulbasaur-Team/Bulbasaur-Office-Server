package ru.bulbasaur.office.infra.ws;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import ru.bulbasaur.office.infra.ws.dto.CatStateOut;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Бульба Кот в main-office: серверная симуляция маршрута.
 * Контрольные точки — {@code classpath:bulba-cat-path.json} (из TMJ polyline);
 * по ним строится замкнутый Catmull-Rom сплайн, и кот идёт по плотной выборке кривой.
 * Синхронизация: {@code Bulba/scripts/sync-bulba-cat-path.sh}.
 */
@Component
public class BulbaCatRegistry {

    public static final String LOCATION_ID = "main-office";

    /** Шаг выборки сплайна (px) — чем меньше, тем плавнее. */
    private static final double SAMPLE_SPACING = 6;
    /** Centripetal Catmull-Rom: меньше петель на острых углах, чем uniform. */
    private static final double SPLINE_ALPHA = 0.5;

    private final List<Point> path;

    private static final double SPEED = 95; // px/s
    private static final long IDLE_MIN_MS = 1000;
    private static final long IDLE_MAX_MS = 2000;
    private static final double WALK_DIST_MIN = 280;
    private static final double WALK_DIST_MAX = 900;

    private double x;
    private double y;
    private boolean facing;
    private boolean moving;
    private int waypointIndex;
    private double distanceLeft;
    private long idleUntilMs = System.currentTimeMillis() + IDLE_MIN_MS;
    /** Сессии, у которых открыт диалог с котом — пока не пусто, кот стоит. */
    private final Set<String> talkers = new HashSet<>();
    /** Была ли прогулка прервана диалогом (чтобы продолжить после). */
    private boolean pausedMidWalk;

    public BulbaCatRegistry(JsonMapper jsonMapper) {
        List<Point> controls = loadControls(jsonMapper);
        this.path = densifyClosedSpline(controls, SAMPLE_SPACING);
        Point start = path.get(0);
        this.x = start.x();
        this.y = start.y();
    }

    public synchronized CatStateOut snapshot() {
        return CatStateOut.of(x, y, facing, moving);
    }

    public synchronized boolean isPaused() {
        return !talkers.isEmpty();
    }

    /**
     * Игрок открыл/закрыл диалог. Возвращает состояние для рассылки, если пауза
     * включилась или снялась (всем нужно увидеть остановку/продолжение).
     */
    public synchronized CatStateOut setTalking(String sessionId, boolean talking) {
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }
        boolean wasPaused = isPaused();
        if (talking) {
            talkers.add(sessionId);
        } else {
            talkers.remove(sessionId);
        }
        boolean nowPaused = isPaused();
        if (!wasPaused && nowPaused) {
            pausedMidWalk = moving || distanceLeft > 0;
            moving = false;
            return snapshot();
        }
        if (wasPaused && !nowPaused) {
            if (pausedMidWalk && distanceLeft > 0) {
                moving = true;
            } else {
                moving = false;
                idleUntilMs = System.currentTimeMillis() + idleMs();
            }
            pausedMidWalk = false;
            return snapshot();
        }
        return null;
    }

    /** Снять диалог сессии (disconnect / уход из локации). */
    public synchronized CatStateOut clearTalking(String sessionId) {
        return setTalking(sessionId, false);
    }

    /**
     * Шаг симуляции. Возвращает состояние, если его нужно разослать
     * (движение или смена idle↔walk); иначе null.
     */
    public synchronized CatStateOut tick(double dt, long nowMs) {
        if (isPaused()) {
            return null;
        }
        if (!moving) {
            if (nowMs < idleUntilMs) {
                return null;
            }
            startWalk();
            return snapshot();
        }

        double stepBudget = SPEED * dt;
        while (stepBudget > 0 && moving) {
            Point target = path.get(nextIndex(waypointIndex));
            double dx = target.x() - x;
            double dy = target.y() - y;
            double dist = Math.hypot(dx, dy);

            if (dist < 0.01) {
                waypointIndex = nextIndex(waypointIndex);
                continue;
            }

            if (Math.abs(dx) > 0.5) {
                facing = dx > 0;
            }

            if (dist <= stepBudget) {
                x = target.x();
                y = target.y();
                waypointIndex = nextIndex(waypointIndex);
                stepBudget -= dist;
                distanceLeft -= dist;
                if (distanceLeft <= 0) {
                    moving = false;
                    idleUntilMs = nowMs + idleMs();
                    break;
                }
            } else {
                double nx = dx / dist;
                double ny = dy / dist;
                x += nx * stepBudget;
                y += ny * stepBudget;
                distanceLeft -= stepBudget;
                stepBudget = 0;
                if (distanceLeft <= 0) {
                    moving = false;
                    idleUntilMs = nowMs + idleMs();
                }
            }
        }
        return snapshot();
    }

    private void startWalk() {
        moving = true;
        distanceLeft = ThreadLocalRandom.current().nextDouble(WALK_DIST_MIN, WALK_DIST_MAX);
        Point next = path.get(nextIndex(waypointIndex));
        if (Math.abs(next.x() - x) > 0.5) {
            facing = next.x() > x;
        }
    }

    private int nextIndex(int i) {
        return (i + 1) % path.size();
    }

    private static long idleMs() {
        return ThreadLocalRandom.current().nextLong(IDLE_MIN_MS, IDLE_MAX_MS + 1);
    }

    private static List<Point> loadControls(JsonMapper jsonMapper) {
        ClassPathResource resource = new ClassPathResource("bulba-cat-path.json");
        try (InputStream in = resource.getInputStream()) {
            JsonNode root = jsonMapper.readTree(in);
            JsonNode pointsNode = root.get("points");
            if (pointsNode == null || !pointsNode.isArray() || pointsNode.size() < 2) {
                throw new IllegalStateException("bulba-cat-path.json: нужно ≥ 2 точек");
            }
            List<Point> points = new ArrayList<>(pointsNode.size());
            for (JsonNode p : pointsNode) {
                points.add(new Point(p.path("x").asDouble(), p.path("y").asDouble()));
            }
            // Замыкающая, совпадающая с первой — лишняя для сплайна.
            if (points.size() >= 2) {
                Point a = points.get(0);
                Point b = points.get(points.size() - 1);
                if (Math.hypot(a.x() - b.x(), a.y() - b.y()) < 0.5) {
                    points.remove(points.size() - 1);
                }
            }
            if (points.size() < 2) {
                throw new IllegalStateException("bulba-cat-path.json: нужно ≥ 2 точек");
            }
            return List.copyOf(points);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось прочитать bulba-cat-path.json", e);
        }
    }

    /**
     * Замкнутый centripetal Catmull-Rom: проходит через контрольные точки,
     * сглаживает углы ломаной. Возвращает плотную выборку с шагом ~spacing.
     */
    static List<Point> densifyClosedSpline(List<Point> controls, double spacing) {
        int n = controls.size();
        if (n == 2) {
            return List.copyOf(controls);
        }
        List<Point> out = new ArrayList<>();
        Point prev = null;
        for (int i = 0; i < n; i++) {
            Point p0 = controls.get((i - 1 + n) % n);
            Point p1 = controls.get(i);
            Point p2 = controls.get((i + 1) % n);
            Point p3 = controls.get((i + 2) % n);

            double segLen = Math.hypot(p2.x() - p1.x(), p2.y() - p1.y());
            int steps = Math.max(1, (int) Math.ceil(segLen / spacing));
            // Не включаем t=1 — следующая сегментная точка p2 станет началом следующего.
            for (int s = 0; s < steps; s++) {
                double t = (double) s / steps;
                Point sample = centripetalCatmullRom(p0, p1, p2, p3, t);
                if (prev == null || Math.hypot(sample.x() - prev.x(), sample.y() - prev.y()) >= spacing * 0.35) {
                    out.add(sample);
                    prev = sample;
                }
            }
        }
        if (out.size() < 2) {
            return List.copyOf(controls);
        }
        return List.copyOf(out);
    }

    /** Centripetal Catmull-Rom между p1 и p2, параметр t ∈ [0, 1]. */
    static Point centripetalCatmullRom(Point p0, Point p1, Point p2, Point p3, double t) {
        double t0 = 0;
        double t1 = tj(t0, p0, p1);
        double t2 = tj(t1, p1, p2);
        double t3 = tj(t2, p2, p3);
        // Вырожденный случай (совпадающие точки) — линейная интерполяция.
        if (t2 - t1 < 1e-9) {
            return new Point(p1.x() + (p2.x() - p1.x()) * t, p1.y() + (p2.y() - p1.y()) * t);
        }
        double tt = t1 + t * (t2 - t1);

        Point a1 = lerpPoint(p0, p1, safeDiv(tt - t0, t1 - t0));
        Point a2 = lerpPoint(p1, p2, safeDiv(tt - t1, t2 - t1));
        Point a3 = lerpPoint(p2, p3, safeDiv(tt - t2, t3 - t2));
        Point b1 = lerpPoint(a1, a2, safeDiv(tt - t0, t2 - t0));
        Point b2 = lerpPoint(a2, a3, safeDiv(tt - t1, t3 - t1));
        return lerpPoint(b1, b2, safeDiv(tt - t1, t2 - t1));
    }

    private static double tj(double ti, Point pi, Point pj) {
        double dx = pj.x() - pi.x();
        double dy = pj.y() - pi.y();
        return ti + Math.pow(Math.hypot(dx, dy), SPLINE_ALPHA);
    }

    private static Point lerpPoint(Point a, Point b, double t) {
        return new Point(a.x() + (b.x() - a.x()) * t, a.y() + (b.y() - a.y()) * t);
    }

    private static double safeDiv(double num, double den) {
        return Math.abs(den) < 1e-9 ? 0 : num / den;
    }

    private record Point(double x, double y) {}
}
