package ru.bulbasaur.office.infra.persistence.repository;

import ru.bulbasaur.office.domain.model.Role;

/** Строка сообщества: логин, роль (null — не выбрана) и число ачивок игрока. */
public interface CommunityRowProjection {

    String getLogin();

    Role getRole();

    long getOwned();
}
