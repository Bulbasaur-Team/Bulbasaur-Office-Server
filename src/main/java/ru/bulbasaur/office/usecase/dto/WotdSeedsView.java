package ru.bulbasaur.office.usecase.dto;

public record WotdSeedsView(GameSeeds guess, GameSeeds wordle) {

    public record GameSeeds(String today, String prev) {
    }
}
