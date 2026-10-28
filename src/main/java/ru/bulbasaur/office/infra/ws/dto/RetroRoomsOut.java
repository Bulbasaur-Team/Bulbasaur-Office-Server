package ru.bulbasaur.office.infra.ws.dto;

import java.util.List;

/** Список активных и прошедших ретро-комнат. */
public record RetroRoomsOut(String type, List<ActiveRoom> active, List<HistoryRoom> history) {

    public record ActiveRoom(
            String id, String name, String adminLogin, int participants, boolean locked, boolean needsPassword
    ) {
    }

    public record HistoryRoom(
            String id, String name, String adminLogin, long closedAt, boolean locked, boolean needsPassword
    ) {
    }

    public static RetroRoomsOut of(List<ActiveRoom> active, List<HistoryRoom> history) {
        return new RetroRoomsOut("retroRooms", active, history);
    }
}
