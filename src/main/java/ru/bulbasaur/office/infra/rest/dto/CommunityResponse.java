package ru.bulbasaur.office.infra.rest.dto;

import ru.bulbasaur.office.domain.model.PlayerAppearance;

import java.util.List;

/** Сообщество: игроки и общее число ачивок в каталоге. */
public record CommunityResponse(List<CommunityPlayerResponse> players, int totalAchievements) {

    /** Игрок сообщества; online — сейчас в игре. */
    public record CommunityPlayerResponse(
            String login,
            PlayerAppearance appearance,
            long owned,
            boolean online
    ) {
    }
}
