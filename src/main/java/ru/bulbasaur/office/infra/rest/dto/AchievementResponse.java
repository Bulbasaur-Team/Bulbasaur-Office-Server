package ru.bulbasaur.office.infra.rest.dto;

/** Ачивка в ответе списка: описание из каталога, признак «получена» и редкость в процентах. */
public record AchievementResponse(
        String code,
        String title,
        String description,
        String image,
        boolean owned,
        double percent) {
}
