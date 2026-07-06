package ru.bulbasaur.office.domain.model;

import java.util.Optional;

public enum Emote {
    WAVE,
    PARTY,
    LIKE,
    LAUGH,
    HEART,
    QUESTION;

    public static Optional<Emote> fromName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(name));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
