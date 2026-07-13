package ru.bulbasaur.office.infra.ws.dto;

/** Предмет в лапах исчез сам (у чашки кофе вышел срок) — руки освободились. */
public record ItemGoneMessage(String itemId) {
}
