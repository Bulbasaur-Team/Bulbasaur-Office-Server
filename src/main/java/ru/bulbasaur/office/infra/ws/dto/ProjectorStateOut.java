package ru.bulbasaur.office.infra.ws.dto;

/** Состояние проектора в локации — шлётся всем в комнате и вошедшему при join/room. */
public record ProjectorStateOut(String type, boolean on, String ownerId, int index) {

    public static ProjectorStateOut off() {
        return new ProjectorStateOut("projectorState", false, null, 0);
    }

    public static ProjectorStateOut on(String ownerId, int index) {
        return new ProjectorStateOut("projectorState", true, ownerId, index);
    }
}
