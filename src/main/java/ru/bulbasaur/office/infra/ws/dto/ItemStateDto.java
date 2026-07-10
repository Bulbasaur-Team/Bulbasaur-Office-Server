package ru.bulbasaur.office.infra.ws.dto;

/** Состояние одного физичного предмета для рассылки клиентам. */
public record ItemStateDto(String id, double x, double y, double vx, double vy) {
}
