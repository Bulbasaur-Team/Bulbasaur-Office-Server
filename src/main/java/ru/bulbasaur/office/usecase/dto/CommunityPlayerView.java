package ru.bulbasaur.office.usecase.dto;

import ru.bulbasaur.office.domain.model.PlayerAppearance;

/** Игрок в списке сообщества: внешний вид, число ачивок и признак «сейчас в игре». */
public record CommunityPlayerView(
        String login,
        PlayerAppearance appearance,
        long ownedAchievements,
        boolean online
) {
}
