package ru.bulbasaur.office.domain.model;

import java.util.Optional;

/** Слот одежды на Бульбазавре. */
public enum WardrobeCategory {
    HAT,
    GLASSES,
    TOP,
    BOTTOM,
    SHOES;

    public static Optional<WardrobeCategory> fromName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        for (WardrobeCategory category : values()) {
            if (category.name().equalsIgnoreCase(name)) {
                return Optional.of(category);
            }
        }
        return Optional.empty();
    }
}
