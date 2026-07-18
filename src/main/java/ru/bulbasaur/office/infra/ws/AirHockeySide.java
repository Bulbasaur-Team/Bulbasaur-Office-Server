package ru.bulbasaur.office.infra.ws;

import java.util.Locale;
import java.util.Optional;

/** Сторона стола аэрохоккея: красная внизу, синяя сверху (абсолютные координаты). */
public enum AirHockeySide {
    RED,
    BLUE;

    public static Optional<AirHockeySide> from(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "red" -> Optional.of(RED);
            case "blue" -> Optional.of(BLUE);
            default -> Optional.empty();
        };
    }

    public String code() {
        return name().toLowerCase(Locale.ROOT);
    }

    public AirHockeySide opposite() {
        return this == RED ? BLUE : RED;
    }
}
