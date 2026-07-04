package ru.bulbasaur.office.usecase.port.out;

import java.util.UUID;

/** Выпуск токена доступа для игрока. Проверка токена — деталь infra (фильтр). */
public interface TokenPort {

    String issue(UUID playerId, String login);
}
