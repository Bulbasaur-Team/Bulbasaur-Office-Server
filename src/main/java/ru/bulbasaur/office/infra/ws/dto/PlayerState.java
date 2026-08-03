package ru.bulbasaur.office.infra.ws.dto;

import ru.bulbasaur.office.domain.model.PlayerAppearance;

/**
 * Состояние одного игрока в комнате для рассылки другим. id — id WebSocket-сессии.
 * heldItemId/heldItemType — предмет в лапах (null, если руки пусты).
 */
public record PlayerState(String id, String login, PlayerAppearance appearance,
                          double x, double y, boolean facing,
                          String heldItemId, String heldItemType) {
}
