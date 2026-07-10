package ru.bulbasaur.office.infra.ws.dto;

import java.util.List;

/** Список активных покер-комнат — ответ на pokerList. */
public record PokerRoomsOut(String type, List<Room> rooms) {

    public record Room(String id, String name, String adminLogin, int participants) {
    }

    public static PokerRoomsOut of(List<Room> rooms) {
        return new PokerRoomsOut("pokerRooms", rooms);
    }
}
