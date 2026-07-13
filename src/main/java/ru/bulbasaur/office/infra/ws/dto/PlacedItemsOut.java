package ru.bulbasaur.office.infra.ws.dto;

import java.util.List;

/** Снапшот предметов на столах комнаты — шлётся вошедшему при join/смене комнаты. */
public record PlacedItemsOut(String type, List<PlacedItemDto> items) {

    public static PlacedItemsOut of(List<PlacedItemDto> items) {
        return new PlacedItemsOut("placedItems", items);
    }
}
