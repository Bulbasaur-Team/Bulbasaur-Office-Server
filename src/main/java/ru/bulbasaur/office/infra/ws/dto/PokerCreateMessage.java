package ru.bulbasaur.office.infra.ws.dto;

/** Создание комнаты: имя и код колоды (sequential, fibonacci, powers, tshirts). */
public record PokerCreateMessage(String name, String scale) {
}
