package ru.bulbasaur.office.usecase.dto;

import java.util.UUID;

/** Один голос завершённого голосования: кто и какую карту выбрал (оценка колоды, «?» или «coffee»). */
public record PokerVoteRecord(UUID playerId, String value) {
}
