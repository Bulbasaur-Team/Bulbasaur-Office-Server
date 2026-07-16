package ru.bulbasaur.office.domain.model;

import java.util.Optional;

/**
 * Мини-игры и способ ранжирования их результата. Внешний код (client, path в REST)
 * оперирует строковым {@code code}, в БД результат хранится по имени enum.
 */
public enum GameId {
    BULBA_JUMP("bulbajump", Direction.HIGHER_BETTER),          // очки, лучший
    BULBA_PACKER("bulbapacker", Direction.HIGHER_BETTER),      // очки, лучший
    BULBA_RACING("bulbaracing", Direction.HIGHER_BETTER),      // очки, лучший
    BULBA_PARKING("bulbaparking", Direction.LOWER_BETTER),     // время, лучший
    BULBA_TANKS("bulbatanks", Direction.HIGHER_BETTER),        // очки, лучший
    BULBA_COLORS("bulbacolors", Direction.HIGHER_BETTER),      // очки, лучший
    BULBA_GUESS("bulbaguess", Direction.HIGHER_BETTER, true),   // всего угаданных слов — накопительно
    BULBA_WORDLE("bulbawordle", Direction.HIGHER_BETTER, true); // всего угаданных слов — накопительно

    private final String code;
    private final Direction direction;
    // true — результаты накапливаются (складываются), а не заменяются лучшим.
    // Направление при этом задаёт только сортировку/ранг.
    private final boolean accumulate;

    GameId(String code, Direction direction) {
        this(code, direction, false);
    }

    GameId(String code, Direction direction, boolean accumulate) {
        this.code = code;
        this.direction = direction;
        this.accumulate = accumulate;
    }

    public String code() {
        return code;
    }

    public Direction direction() {
        return direction;
    }

    public boolean accumulate() {
        return accumulate;
    }

    public static Optional<GameId> fromCode(String code) {
        for (GameId game : values()) {
            if (game.code.equals(code)) {
                return Optional.of(game);
            }
        }
        return Optional.empty();
    }
}
