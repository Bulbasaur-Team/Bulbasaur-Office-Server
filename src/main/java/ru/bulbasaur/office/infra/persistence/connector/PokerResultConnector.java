package ru.bulbasaur.office.infra.persistence.connector;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.infra.persistence.entity.PlayerEntity;
import ru.bulbasaur.office.infra.persistence.entity.PokerTaskEntity;
import ru.bulbasaur.office.infra.persistence.entity.PokerVoteEntity;
import ru.bulbasaur.office.infra.persistence.repository.PokerTaskJpaRepository;
import ru.bulbasaur.office.infra.persistence.repository.PokerVoteJpaRepository;
import ru.bulbasaur.office.usecase.dto.PokerVoteRecord;
import ru.bulbasaur.office.usecase.dto.PokerVotingUpsert;
import ru.bulbasaur.office.usecase.port.out.PokerResultRepositoryPort;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PokerResultConnector implements PokerResultRepositoryPort {

    private final PokerTaskJpaRepository tasks;
    private final PokerVoteJpaRepository votes;

    @Override
    @Transactional
    public void save(PokerVotingUpsert upsert) {
        PokerTaskEntity task = new PokerTaskEntity();
        task.setId(UUID.randomUUID());
        task.setRoomName(upsert.roomName());
        task.setTitle(upsert.taskTitle());
        task.setAverage(upsert.average());
        task.setRecommended(upsert.recommended());
        task.setCreatedAt(Instant.now());
        tasks.save(task);

        for (PokerVoteRecord record : upsert.votes()) {
            PokerVoteEntity vote = new PokerVoteEntity();
            vote.setId(UUID.randomUUID());
            vote.setTask(task);
            PlayerEntity playerRef = new PlayerEntity();
            playerRef.setId(record.playerId());
            vote.setPlayer(playerRef);
            vote.setValue(record.value());
            votes.save(vote);
        }
    }
}
