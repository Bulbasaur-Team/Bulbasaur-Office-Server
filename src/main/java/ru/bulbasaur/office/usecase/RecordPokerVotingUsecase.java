package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bulbasaur.office.domain.model.PokerScale;
import ru.bulbasaur.office.usecase.dto.PokerVoteRecord;
import ru.bulbasaur.office.usecase.dto.PokerVotingResult;
import ru.bulbasaur.office.usecase.dto.PokerVotingUpsert;
import ru.bulbasaur.office.usecase.dto.RecordPokerVotingCommand;
import ru.bulbasaur.office.usecase.port.out.PokerRepositoryPort;

import java.util.List;

/**
 * Итог голосования planning poker по колоде комнаты. «?» и «coffee» не
 * учитываются. Рекомендация — ближайшая карта колоды (при равном расстоянии
 * берём большую). У футболок в среднее идут порядки размеров, наружу уходит
 * только размер. Результат вместе с голосами сохраняется в БД.
 */
@Service
@RequiredArgsConstructor
public class RecordPokerVotingUsecase {

    private final PokerRepositoryPort pokerPort;

    public PokerVotingResult execute(RecordPokerVotingCommand command) {
        PokerVotingResult result = summarize(command.scale(), command.votes());
        pokerPort.save(PokerVotingUpsert.builder()
                .roomId(command.roomId())
                .roomName(command.roomName())
                .taskTitle(command.taskTitle())
                .average(result.average())
                .recommended(result.recommended())
                .votes(command.votes())
                .build());
        return result;
    }

    private PokerVotingResult summarize(PokerScale scale, List<PokerVoteRecord> votes) {
        PokerScale deck = scale == null ? PokerScale.FIBONACCI : scale;
        double sum = 0;
        int count = 0;
        for (PokerVoteRecord vote : votes) {
            Double weight = deck.weight(vote.value());
            if (weight != null) {
                sum += weight;
                count++;
            }
        }
        if (count == 0) {
            return new PokerVotingResult(null, null);
        }
        double average = sum / count;
        String recommended = deck.nearest(average);
        Double storedAverage = deck.numericAverage() ? average : null;
        return new PokerVotingResult(storedAverage, recommended);
    }
}
