package ru.bulbasaur.office.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

/** Сохранённая задача planning poker с голосами. */
@Getter
@Builder
@AllArgsConstructor
public class PokerTask {

    private final UUID id;
    private final String title;
    private final Double average;
    private final Integer recommended;
    private final List<PokerVote> votes;
}
