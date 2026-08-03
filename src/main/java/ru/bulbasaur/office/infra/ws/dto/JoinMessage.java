package ru.bulbasaur.office.infra.ws.dto;

/** Игрок вошёл в мир мультиплеера: стартовая локация и позиция. */
public record JoinMessage(String locationId, double x, double y, boolean facing) {
}
