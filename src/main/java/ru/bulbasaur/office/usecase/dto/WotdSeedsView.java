package ru.bulbasaur.office.usecase.dto;

import java.time.LocalDate;

public record WotdSeedsView(GameSeeds guess, GameSeeds wordle, LocalDate today) {

    public record GameSeeds(String today, String prev) {
    }
}
