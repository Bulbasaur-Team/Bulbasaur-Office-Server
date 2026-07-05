package ru.bulbasaur.office.infra.ws.dto;

/** Игрок перешёл в другую локацию (комнату). */
public record RoomMessage(String locationId, double x, double y, boolean facing) {
}
