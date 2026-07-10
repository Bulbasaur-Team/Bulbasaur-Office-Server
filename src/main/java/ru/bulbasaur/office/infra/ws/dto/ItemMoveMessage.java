package ru.bulbasaur.office.infra.ws.dto;

/** Репорт позиции предмета от его владельца (последнего ударившего). */
public record ItemMoveMessage(String itemId, double x, double y, double vx, double vy) {
}
