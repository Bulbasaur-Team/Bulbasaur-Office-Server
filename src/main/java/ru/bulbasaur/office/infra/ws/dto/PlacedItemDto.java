package ru.bulbasaur.office.infra.ws.dto;

/**
 * Предмет, поставленный на стол (слой tables): виден всем в локации.
 * tableIndex — номер места из карты, на одно место можно поставить только один предмет.
 * expiresAt — момент исчезновения (epoch ms).
 */
public record PlacedItemDto(String id, String type, int tableIndex, double x, double y, long expiresAt) {
}
