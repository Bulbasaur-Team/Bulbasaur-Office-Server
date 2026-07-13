package ru.bulbasaur.office.infra.ws.dto;

/**
 * Состояние одного игрока в комнате для рассылки другим. id — id WebSocket-сессии.
 * heldItemId/heldItemType — предмет в лапах (null, если руки пусты): остальные рисуют
 * его на игроке, поэтому предмет виден и когда игрок приходит с ним из другой локации.
 */
public record PlayerState(String id, String login, String role, double x, double y, boolean facing,
                          String heldItemId, String heldItemType) {
}
