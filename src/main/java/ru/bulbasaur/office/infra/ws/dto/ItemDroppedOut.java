package ru.bulbasaur.office.infra.ws.dto;

/**
 * Предмет бросили на пол — остальные создают его заново в этой точке (пока он был
 * в лапах, в их мире его не было).
 */
public record ItemDroppedOut(String type, String itemId, String itemType, double x, double y) {

    public static ItemDroppedOut of(String itemId, String itemType, double x, double y) {
        return new ItemDroppedOut("itemDropped", itemId, itemType, x, y);
    }
}
