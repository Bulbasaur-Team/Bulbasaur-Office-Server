package ru.bulbasaur.office.usecase.quiz;

import java.time.Duration;
import java.time.Instant;

/** Константы и реген энергии Bulba Quiz. */
public final class QuizConstants {

    public static final int MAX_ENERGY = 5;
    public static final Duration ENERGY_REGEN = Duration.ofMinutes(30);
    public static final int ENERGY_PRICE = 100;
    public static final int REROLL_PRICE = 200;
    public static final int FIFTY_PRICE = 150;
    public static final int QUESTIONS_PER_ATTEMPT = 3;
    public static final Duration ANSWER_TIME = Duration.ofSeconds(20);
    public static final Duration CORRECT_FEEDBACK_TIME = Duration.ofMillis(800);
    public static final Duration ANSWER_GRACE = Duration.ofSeconds(2);
    public static final int CHEST_EVERY = 5;
    /** Bulba Coins из сундука: 300..1000 с шагом 100. */
    public static final int CHEST_COINS_STEP = 100;
    public static final int CHEST_COINS_MIN = 300;
    public static final int CHEST_COINS_MAX = 1000;
    public static final int CHEST_ENERGY_MIN = 1;
    public static final int CHEST_ENERGY_MAX = 3;
    /** Шанс выпадения одного предмета одежды из сундука. */
    public static final double CHEST_WARDROBE_CHANCE = 0.25;

    private QuizConstants() {
    }

    /**
     * Применяет пассивный реген: возвращает новое значение энергии и обновлённый timestamp.
     * Частичный прогресс до следующей единицы сохраняется.
     */
    public static RegenResult applyRegen(int energy, Instant energyUpdatedAt, Instant now) {
        if (energy >= MAX_ENERGY) {
            return new RegenResult(MAX_ENERGY, now);
        }
        long regenMs = ENERGY_REGEN.toMillis();
        long elapsed = Math.max(0, Duration.between(energyUpdatedAt, now).toMillis());
        long gained = elapsed / regenMs;
        if (gained <= 0) {
            return new RegenResult(energy, energyUpdatedAt);
        }
        int next = (int) Math.min(MAX_ENERGY, energy + gained);
        Instant updated = energyUpdatedAt.plusMillis(gained * regenMs);
        if (next >= MAX_ENERGY) {
            return new RegenResult(MAX_ENERGY, now);
        }
        return new RegenResult(next, updated);
    }

    public static Instant nextEnergyAt(int energy, Instant energyUpdatedAt) {
        if (energy >= MAX_ENERGY) {
            return null;
        }
        return energyUpdatedAt.plus(ENERGY_REGEN);
    }

    public record RegenResult(int energy, Instant energyUpdatedAt) {
    }
}
