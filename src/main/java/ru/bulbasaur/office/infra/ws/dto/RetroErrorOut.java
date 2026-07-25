package ru.bulbasaur.office.infra.ws.dto;

public record RetroErrorOut(String type, String message) {

    public static RetroErrorOut of(String message) {
        return new RetroErrorOut("retroError", message);
    }
}
