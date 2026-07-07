package ru.bulbasaur.office.infra.rest.dto;

public record WotdSeedsResponse(GameSeedsResponse guess, GameSeedsResponse wordle) {

    public record GameSeedsResponse(String today, String prev) {
    }
}
