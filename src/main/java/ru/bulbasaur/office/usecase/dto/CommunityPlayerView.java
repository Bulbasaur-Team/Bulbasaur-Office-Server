package ru.bulbasaur.office.usecase.dto;

import ru.bulbasaur.office.domain.model.Role;

/** Игрок в списке сообщества: роль (null — не выбрана), число ачивок и признак «сейчас в игре». */
public record CommunityPlayerView(String login, Role role, long ownedAchievements, boolean online) {
}
