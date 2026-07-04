package ru.bulbasaur.office.domain.model;

import java.util.UUID;

/** Игрок как доменная сущность (без чувствительных данных). */
public record Player(UUID id, String login) {
}
