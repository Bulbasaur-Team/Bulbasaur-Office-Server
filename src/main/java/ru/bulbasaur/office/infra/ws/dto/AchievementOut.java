package ru.bulbasaur.office.infra.ws.dto;

import ru.bulbasaur.office.domain.model.Achievement;

/** Попап о свежевыданной ачивке (картинка, название, описание). */
public record AchievementOut(String type, String code, String title, String description, String image) {

    public static AchievementOut of(Achievement achievement) {
        return new AchievementOut("achievement", achievement.code(), achievement.title(),
                achievement.description(), achievement.image());
    }
}
