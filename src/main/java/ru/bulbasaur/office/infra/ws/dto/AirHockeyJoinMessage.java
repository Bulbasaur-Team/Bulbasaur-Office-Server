package ru.bulbasaur.office.infra.ws.dto;

/** Занять сторону стола аэрохоккея: {@code side} = {@code red} | {@code blue}. */
public record AirHockeyJoinMessage(String side) {
}
