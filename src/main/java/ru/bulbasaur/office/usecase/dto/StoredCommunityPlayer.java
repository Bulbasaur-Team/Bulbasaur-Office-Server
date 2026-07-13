package ru.bulbasaur.office.usecase.dto;

import ru.bulbasaur.office.domain.model.Role;

/** Строка сообщества из хранилища: роль (null — не выбрана) и число полученных ачивок. */
public record StoredCommunityPlayer(String login, Role role, long ownedAchievements) {
}
