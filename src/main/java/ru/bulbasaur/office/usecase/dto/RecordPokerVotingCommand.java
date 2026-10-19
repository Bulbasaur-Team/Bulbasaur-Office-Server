package ru.bulbasaur.office.usecase.dto;

import lombok.Builder;
import ru.bulbasaur.office.domain.model.PokerScale;

import java.util.List;
import java.util.UUID;

/** Завершённое голосование planning poker: задача, комната, колода и все отданные голоса. */
@Builder
public record RecordPokerVotingCommand(UUID roomId, String roomName, String taskTitle,
                                       PokerScale scale, List<PokerVoteRecord> votes) {
}
