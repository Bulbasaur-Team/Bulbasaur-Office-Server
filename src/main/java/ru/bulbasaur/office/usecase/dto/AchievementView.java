package ru.bulbasaur.office.usecase.dto;

/** Ачивка в списке игрока: описание из каталога, признак «получена» и редкость. */
public record AchievementView(
        String code,
        String title,
        String description,
        String image,
        boolean owned,
        double percent) {
}
