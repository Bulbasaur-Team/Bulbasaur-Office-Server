package ru.bulbasaur.office.usecase.dto;

import java.util.List;

/** Завершённое голосование planning poker: задача, комната и все отданные голоса. */
public record RecordPokerVotingCommand(String roomName, String taskTitle, List<PokerVoteRecord> votes) {
}
