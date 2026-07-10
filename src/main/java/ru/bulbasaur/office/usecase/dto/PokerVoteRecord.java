package ru.bulbasaur.office.usecase.dto;

import java.util.UUID;

/** Один голос завершённого голосования: кто и какую карту выбрал ("0".."13", "?", "coffee"). */
public record PokerVoteRecord(UUID playerId, String value) {
}
