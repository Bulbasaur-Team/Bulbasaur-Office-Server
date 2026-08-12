package ru.bulbasaur.office.infra.ws.dto;

import lombok.Builder;

import java.util.List;

/** Список активных и прошедших покер-комнат — ответ на pokerList. */
@Builder
public record PokerRoomsOut(String type, List<Room> active, List<HistoryRoom> history) {

    @Builder
    public record Room(String id, String name, String adminLogin, int participants) {
    }

    @Builder
    public record HistoryRoom(String id, String name, String adminLogin, long closedAt) {
    }
}
