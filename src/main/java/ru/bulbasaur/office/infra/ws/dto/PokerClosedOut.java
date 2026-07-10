package ru.bulbasaur.office.infra.ws.dto;

/** Комната закрыта админом или по TTL — участники возвращаются в лобби. */
public record PokerClosedOut(String type, String roomId) {

    public static PokerClosedOut of(String roomId) {
        return new PokerClosedOut("pokerClosed", roomId);
    }
}
