package ru.bulbasaur.office.usecase.dto;

import java.util.List;

public record WotdProgressView(boolean solved, int attempts, List<String> guesses) {

    public static WotdProgressView empty() {
        return new WotdProgressView(false, 0, List.of());
    }
}
