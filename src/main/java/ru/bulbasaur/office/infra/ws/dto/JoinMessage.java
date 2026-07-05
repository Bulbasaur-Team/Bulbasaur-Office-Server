package ru.bulbasaur.office.infra.ws.dto;

/** Игрок вошёл в мир мультиплеера: роль, стартовая локация и позиция. */
public record JoinMessage(String role, String locationId, double x, double y, boolean facing) {
}
