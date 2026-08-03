package ru.bulbasaur.office.infra.persistence.repository;

import java.util.UUID;

/** Строка сообщества: id, логин и число ачивок игрока. */
public interface CommunityRowProjection {

    UUID getId();

    String getLogin();

    long getOwned();
}
