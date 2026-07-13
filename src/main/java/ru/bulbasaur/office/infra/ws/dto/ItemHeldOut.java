package ru.bulbasaur.office.infra.ws.dto;

/** Игрок держит предмет в лапах — остальные рисуют его на этом игроке. */
public record ItemHeldOut(String type, String id, String itemId, String itemType) {

    public static ItemHeldOut of(String sessionId, String itemId, String itemType) {
        return new ItemHeldOut("itemHeld", sessionId, itemId, itemType);
    }
}
