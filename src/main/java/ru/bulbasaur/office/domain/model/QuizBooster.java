package ru.bulbasaur.office.domain.model;

import java.util.Arrays;
import java.util.Optional;

/** Коды бустеров Bulba Quiz, используемые одинаково в REST API и на клиенте. */
public enum QuizBooster {
    REROLL("reroll"),
    FIFTY("fifty");

    private final String code;

    QuizBooster(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static Optional<QuizBooster> fromCode(String code) {
        return Arrays.stream(values())
                .filter(booster -> booster.code.equals(code))
                .findFirst();
    }
}
