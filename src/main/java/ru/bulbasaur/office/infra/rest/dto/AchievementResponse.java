package ru.bulbasaur.office.infra.rest.dto;

/** Ачивка в ответе списка: описание из каталога и признак «получена». */
public record AchievementResponse(
        String code,
        String title,
        String description,
        String image,
        boolean owned) {
}
