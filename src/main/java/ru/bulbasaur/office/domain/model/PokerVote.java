package ru.bulbasaur.office.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/** Голос игрока в сохранённой задаче planning poker. */
@Getter
@Builder
@AllArgsConstructor
public class PokerVote {

    private final UUID playerId;
    private final String value;
}
