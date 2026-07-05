package ru.bulbasaur.office.infra.ws.dto;

/** В комнату вошёл новый игрок — рассылается остальным в этой комнате. */
public record JoinedOut(String type, PlayerState player) {

    public static JoinedOut of(PlayerState player) {
        return new JoinedOut("joined", player);
    }
}
