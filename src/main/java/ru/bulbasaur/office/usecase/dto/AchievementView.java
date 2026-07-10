package ru.bulbasaur.office.usecase.dto;

/** Ачивка в списке игрока: описание из каталога плюс признак «получена». */
public record AchievementView(
        String code,
        String title,
        String description,
        String image,
        boolean owned) {
}
