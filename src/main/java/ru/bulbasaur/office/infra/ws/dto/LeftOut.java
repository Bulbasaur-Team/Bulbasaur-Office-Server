package ru.bulbasaur.office.infra.ws.dto;

/** Игрок покинул комнату (ушёл в другую локацию или отключился). */
public record LeftOut(String type, String id) {

    public static LeftOut of(String id) {
        return new LeftOut("left", id);
    }
}
