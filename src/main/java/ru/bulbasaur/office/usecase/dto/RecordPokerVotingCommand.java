package ru.bulbasaur.office.usecase.dto;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

/** Завершённое голосование planning poker: задача, комната и все отданные голоса. */
@Builder
public record RecordPokerVotingCommand(UUID roomId, String roomName, String taskTitle,
                                       List<PokerVoteRecord> votes) {
}
