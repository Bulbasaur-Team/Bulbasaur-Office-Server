package ru.bulbasaur.office.infra.ws.dto;

/** Состояние одного игрока в комнате для рассылки другим. id — id WebSocket-сессии. */
public record PlayerState(String id, String login, String role, double x, double y, boolean facing) {
}
