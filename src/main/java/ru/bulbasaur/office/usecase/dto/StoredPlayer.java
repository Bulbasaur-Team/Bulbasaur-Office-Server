package ru.bulbasaur.office.usecase.dto;

import java.util.UUID;

/** Игрок с хешем пароля — нужен usecase-слою для проверки логина. */
public record StoredPlayer(UUID id, String login, String passwordHash) {
}
