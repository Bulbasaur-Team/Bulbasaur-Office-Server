package ru.bulbasaur.office.infra.ws.dto;

/** Игрок больше ничего не держит — остальные убирают предмет с его лап. */
public record ItemReleasedOut(String type, String id) {

    public static ItemReleasedOut of(String sessionId) {
        return new ItemReleasedOut("itemReleased", sessionId);
    }
}
