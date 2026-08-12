package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bulbasaur.office.usecase.dto.PokerVoteRecord;
import ru.bulbasaur.office.usecase.dto.PokerVotingResult;
import ru.bulbasaur.office.usecase.dto.PokerVotingUpsert;
import ru.bulbasaur.office.usecase.dto.RecordPokerVotingCommand;
import ru.bulbasaur.office.usecase.port.out.PokerRepositoryPort;

import java.util.List;

/**
 * Итог голосования planning poker: средняя оценка по числовым голосам ("?" и
 * "coffee" не учитываются), рекомендация — ближайшее число Фибоначчи (при равном
 * расстоянии берём большее). Результат вместе с голосами сохраняется в БД.
 */
@Service
@RequiredArgsConstructor
public class RecordPokerVotingUsecase {

    private static final int[] FIBONACCI = {0, 1, 2, 3, 5, 8, 13};

    private final PokerRepositoryPort pokerPort;

    public PokerVotingResult execute(RecordPokerVotingCommand command) {
        PokerVotingResult result = summarize(command.votes());
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

    private PokerVotingResult summarize(List<PokerVoteRecord> votes) {
        double sum = 0;
        int count = 0;
        for (PokerVoteRecord vote : votes) {
            Integer numeric = asNumeric(vote.value());
            if (numeric != null) {
                sum += numeric;
                count++;
            }
        }
        if (count == 0) {
            return new PokerVotingResult(null, null);
        }
        double average = sum / count;
        return new PokerVotingResult(average, nearestFibonacci(average));
    }

    private Integer asNumeric(String value) {
        for (int f : FIBONACCI) {
            if (String.valueOf(f).equals(value)) {
                return f;
            }
        }
        return null;
    }

    private int nearestFibonacci(double average) {
        int nearest = FIBONACCI[0];
        for (int f : FIBONACCI) {
            if (Math.abs(f - average) <= Math.abs(nearest - average)) {
                nearest = f;
            }
        }
        return nearest;
    }
}
