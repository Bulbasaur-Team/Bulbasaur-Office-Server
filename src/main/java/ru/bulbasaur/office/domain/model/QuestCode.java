package ru.bulbasaur.office.domain.model;

import java.util.Optional;

/**
 * Квесты мультиплеера. Внешний код (client, path в REST) оперирует строковым {@code code}.
 */
public enum QuestCode {
    FRIDGE_PIN("fridge_pin", "Пинкод холодильника", "696967", 20_000L, "Квест «Пинкод холодильника»", 5),
    LOST_PACKAGE("lost_package", "Посылка не туда", "LOVESHOT", 15_000L, "Квест «Посылка не туда»", 10);

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

    /** Другой квест, который должен быть COMPLETED, иначе этот заблокирован. */
    public Optional<QuestCode> requiresCompleted() {
        return switch (this) {
            case LOST_PACKAGE -> Optional.of(FRIDGE_PIN);
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
