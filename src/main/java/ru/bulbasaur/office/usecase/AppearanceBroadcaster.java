package ru.bulbasaur.office.usecase;

import ru.bulbasaur.office.domain.model.PlayerAppearance;

import java.util.UUID;

/** Рассылка смены внешности по WebSocket (реализация в infra/ws). */
public interface AppearanceBroadcaster {

    void broadcast(UUID playerId, PlayerAppearance appearance);
}
