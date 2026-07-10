package ru.bulbasaur.office.usecase.port.out;

import ru.bulbasaur.office.domain.model.Achievement;

import java.util.UUID;

/** Уведомление игрока о свежевыданной ачивке (всплывающий попап, если игрок онлайн). */
public interface AchievementNotifierPort {

    void notifyGranted(UUID playerId, Achievement achievement);
}
