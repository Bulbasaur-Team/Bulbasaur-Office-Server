package ru.bulbasaur.office.infra.ws.dto;

/**
 * Игрок бросил предмет из лап на пол в точке (x, y). itemType нужен остальным:
 * пока предмет был в лапах, они его у себя уничтожили и теперь создают заново.
 */
public record ItemDropMessage(String itemId, String itemType, double x, double y) {
}
