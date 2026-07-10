package ru.bulbasaur.office.infra.ws.dto;

/** Отказ покер-операции (комнаты уже нет, не админ и т.п.) — показывается игроку. */
public record PokerErrorOut(String type, String message) {

    public static PokerErrorOut of(String message) {
        return new PokerErrorOut("pokerError", message);
    }
}
