package ru.bulbasaur.office.usecase.dto;

import java.util.List;

/** Готовая к сохранению запись голосования: задача с итогом и голосами. */
public record PokerVotingUpsert(String roomName, String taskTitle,
                                Double average, Integer recommended,
                                List<PokerVoteRecord> votes) {
}
