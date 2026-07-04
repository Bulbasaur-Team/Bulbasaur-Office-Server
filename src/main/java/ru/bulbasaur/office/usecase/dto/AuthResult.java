package ru.bulbasaur.office.usecase.dto;

/** Результат регистрации/входа: выданный токен и логин игрока. */
public record AuthResult(String token, String login) {
}
