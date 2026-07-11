package ru.bulbasaur.office.infra.rest.dto;

/** Профиль текущего игрока; role == null — роль ещё не выбрана. */
public record ProfileResponse(String login, String role) {
}
