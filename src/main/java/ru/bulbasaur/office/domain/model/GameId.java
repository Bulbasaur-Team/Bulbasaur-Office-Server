package ru.bulbasaur.office.domain.model;

import java.util.Optional;

/**
 * Мини-игры и способ ранжирования их результата. Внешний код (client, path в REST)
 * оперирует строковым {@code code}, в БД результат хранится по имени enum.
 */
public enum GameId {
    BULBA_JUMP("bulbajump", Direction.HIGHER_BETTER),      // очки
    BULBA_PACKER("bulbapacker", Direction.HIGHER_BETTER),  // очки
    BULBA_RACING("bulbaracing", Direction.HIGHER_BETTER),  // очки
    BULBA_PARKING("bulbaparking", Direction.LOWER_BETTER), // время
    BULBA_GUESS("bulbaguess", Direction.LOWER_BETTER),     // меньше попыток
    BULBA_WORDLE("bulbawordle", Direction.HIGHER_BETTER);  // больше угаданных слов

    private final String code;
    private final Direction direction;

    GameId(String code, Direction direction) {
        this.code = code;
        this.direction = direction;
    }

    public String code() {
        return code;
    }

    public Direction direction() {
        return direction;
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
