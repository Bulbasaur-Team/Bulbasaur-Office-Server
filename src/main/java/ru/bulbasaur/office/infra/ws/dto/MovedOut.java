package ru.bulbasaur.office.infra.ws.dto;

/** Игрок сдвинулся — рассылается остальным в его комнате. */
public record MovedOut(String type, String id, double x, double y, boolean facing) {

    public static MovedOut of(String id, double x, double y, boolean facing) {
        return new MovedOut("moved", id, x, y, facing);
    }
}
