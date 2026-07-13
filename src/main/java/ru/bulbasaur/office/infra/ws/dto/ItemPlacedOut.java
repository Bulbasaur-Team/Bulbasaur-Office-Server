package ru.bulbasaur.office.infra.ws.dto;

/** На стол в комнате поставили предмет — его видят все в локации. */
public record ItemPlacedOut(String type, PlacedItemDto item) {

    public static ItemPlacedOut of(PlacedItemDto item) {
        return new ItemPlacedOut("itemPlaced", item);
    }
}
