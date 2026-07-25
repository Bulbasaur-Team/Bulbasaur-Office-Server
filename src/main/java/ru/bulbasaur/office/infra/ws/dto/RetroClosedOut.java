package ru.bulbasaur.office.infra.ws.dto;

public record RetroClosedOut(String type, String roomId) {

    public static RetroClosedOut of(String roomId) {
        return new RetroClosedOut("retroClosed", roomId);
    }
}
