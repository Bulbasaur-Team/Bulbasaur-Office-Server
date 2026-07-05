package ru.bulbasaur.office.infra.ws.dto;

/** Игрок сдвинулся внутри текущей локации. */
public record MoveMessage(double x, double y, boolean facing) {
}
