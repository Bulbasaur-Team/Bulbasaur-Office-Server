package ru.bulbasaur.office.usecase.dto;

import java.util.UUID;

/** Строка сообщества из хранилища: id, логин и число полученных ачивок. */
public record StoredCommunityPlayer(UUID id, String login, long ownedAchievements) {
}
