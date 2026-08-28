package ru.bulbasaur.office.domain.model;

import java.util.Optional;

/**
 * Квесты сюжета. Внешний код (client, path в REST) оперирует строковым {@code code}.
 * Пройти можно только по порядку: каждый квест после первого требует предыдущий COMPLETED.
 */
public enum QuestCode {
    PROLOGUE("prologue", "Пролог", "", 0L, "", 0),
    ADAPTATION("adaptation", "Адаптация", "", 0L, "", 3),
    FRIDGE_PIN("fridge_pin", "Пинкод холодильника", "696967", 10_000L, "Квест «Пинкод холодильника»", 5),
    GREEN_ALERT("green_alert", "Зелёный дашборд", "GREEN", 5_000L, "Квест «Зелёный дашборд»", 5),
    LOST_PACKAGE("lost_package", "Посылка не туда", "LOVESHOT", 8_000L, "Квест «Посылка не туда»", 10),
    STRATEGY("strategy", "Стратегическое планирование", "", 0L, "", 10),
    RECONCILIATION("reconciliation", "Сверка", "", 0L, "", 10),
    PRESENTATION("presentation", "Сборка презентации", "", 0L, "", 10),
    QUANTUM("quantum", "Квантовая физика", "", 0L, "", 10),
    DAY_X("day_x", "День X", "", 20_000L, "Квест «День X»", 10);

    private final String code;
    private final String title;
    private final String pin;
    private final long rewardBc;
    private final String rewardTitle;
    /** Минимум полученных ачивок, чтобы квест стал доступен. */
    private final int minAchievements;

    QuestCode(String code, String title, String pin, long rewardBc, String rewardTitle, int minAchievements) {
        this.code = code;
        this.title = title;
        this.pin = pin;
        this.rewardBc = rewardBc;
        this.rewardTitle = rewardTitle;
        this.minAchievements = minAchievements;
    }

    public String code() {
        return code;
    }

    public String title() {
        return title;
    }

    public String pin() {
        return pin;
    }

    public long rewardBc() {
        return rewardBc;
    }

    public String rewardTitle() {
        return rewardTitle;
    }

    public int minAchievements() {
        return minAchievements;
    }

    public boolean hasSecret() {
        return pin != null && !pin.isBlank();
    }

    public boolean hasReward() {
        return rewardBc > 0;
    }

    /** Другой квест, который должен быть COMPLETED, иначе этот заблокирован. */
    public Optional<QuestCode> requiresCompleted() {
        return switch (this) {
            case ADAPTATION -> Optional.of(PROLOGUE);
            case FRIDGE_PIN -> Optional.of(ADAPTATION);
            case GREEN_ALERT -> Optional.of(FRIDGE_PIN);
            case LOST_PACKAGE -> Optional.of(GREEN_ALERT);
            case STRATEGY -> Optional.of(LOST_PACKAGE);
            case RECONCILIATION -> Optional.of(STRATEGY);
            case PRESENTATION -> Optional.of(RECONCILIATION);
            case QUANTUM -> Optional.of(PRESENTATION);
            case DAY_X -> Optional.of(QUANTUM);
            default -> Optional.empty();
        };
    }

    public static Optional<QuestCode> fromCode(String code) {
        for (QuestCode quest : values()) {
            if (quest.code.equals(code)) {
                return Optional.of(quest);
            }
        }
        return Optional.empty();
    }
}
