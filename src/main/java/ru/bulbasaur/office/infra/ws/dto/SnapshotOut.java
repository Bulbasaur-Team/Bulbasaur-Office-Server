package ru.bulbasaur.office.infra.ws.dto;

import java.util.List;

/** Полный список остальных игроков в комнате — шлётся вошедшему при join/смене комнаты. */
public record SnapshotOut(String type, List<PlayerState> players) {

    public static SnapshotOut of(List<PlayerState> players) {
        return new SnapshotOut("snapshot", players);
    }
}
