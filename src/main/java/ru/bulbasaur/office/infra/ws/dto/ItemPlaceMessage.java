package ru.bulbasaur.office.infra.ws.dto;

/** Игрок поставил предмет из лап на стол (место tableIndex из слоя tables карты). */
public record ItemPlaceMessage(String itemId, String itemType, int tableIndex, double x, double y) {
}
