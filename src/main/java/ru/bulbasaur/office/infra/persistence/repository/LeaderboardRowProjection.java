package ru.bulbasaur.office.infra.persistence.repository;

import java.util.UUID;

/** Проекция строки лидерборда: логин игрока, его id и результат. */
public interface LeaderboardRowProjection {

    String getLogin();

    UUID getPlayerId();

    long getValue();
}
