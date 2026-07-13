package ru.bulbasaur.office.infra.ws.dto;

/** Игрок взял предмет в лапы (мяч с пола, чашку со стола или свежую чашку с кухни). */
public record ItemGrabMessage(String itemId, String itemType) {
}
