package ru.bulbasaur.office.infra.rest.dto;

import java.util.List;

public record WotdProgressResponse(boolean solved, int attempts, List<String> guesses) {
}
