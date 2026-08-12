package ru.bulbasaur.office.usecase.dto;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

/** Готовая к сохранению запись голосования: задача с итогом и голосами. */
@Builder
public record PokerVotingUpsert(UUID roomId, String roomName, String taskTitle,
                                Double average, Integer recommended,
                                List<PokerVoteRecord> votes) {
}
