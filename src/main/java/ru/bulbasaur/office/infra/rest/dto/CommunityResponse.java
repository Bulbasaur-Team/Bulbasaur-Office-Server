package ru.bulbasaur.office.infra.rest.dto;

import java.util.List;

/** Сообщество: игроки в порядке регистрации и общее число ачивок в каталоге. */
public record CommunityResponse(List<CommunityPlayerResponse> players, int totalAchievements) {

    /** Игрок сообщества; role == null — роль ещё не выбрана. */
    public record CommunityPlayerResponse(String login, String role, long owned) {
    }
}
