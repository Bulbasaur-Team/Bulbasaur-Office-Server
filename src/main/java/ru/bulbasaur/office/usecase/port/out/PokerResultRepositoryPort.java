package ru.bulbasaur.office.usecase.port.out;

import ru.bulbasaur.office.usecase.dto.PokerVotingUpsert;

public interface PokerResultRepositoryPort {

    void save(PokerVotingUpsert upsert);
}
