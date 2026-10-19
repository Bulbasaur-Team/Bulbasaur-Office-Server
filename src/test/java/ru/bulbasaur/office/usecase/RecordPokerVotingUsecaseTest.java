package ru.bulbasaur.office.usecase;

import org.junit.jupiter.api.Test;
import ru.bulbasaur.office.domain.model.PokerScale;
import ru.bulbasaur.office.usecase.dto.PokerVoteRecord;
import ru.bulbasaur.office.usecase.dto.PokerVotingResult;
import ru.bulbasaur.office.usecase.dto.PokerVotingUpsert;
import ru.bulbasaur.office.usecase.dto.RecordPokerVotingCommand;
import ru.bulbasaur.office.usecase.port.out.PokerRepositoryPort;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RecordPokerVotingUsecaseTest {

    private final PokerRepositoryPort port = mock(PokerRepositoryPort.class);
    private final RecordPokerVotingUsecase usecase = new RecordPokerVotingUsecase(port);

    @Test
    void fibonacciIgnoresBreaksAndPicks21OnTie() {
        PokerVotingResult result = usecase.execute(command(PokerScale.FIBONACCI, "13", "21", "?", "coffee"));

        assertThat(result.average()).isEqualTo(17.0);
        assertThat(result.recommended()).isEqualTo("21");
    }

    @Test
    void sequentialUsesZeroThroughTen() {
        PokerVotingResult result = usecase.execute(command(PokerScale.SEQUENTIAL, "0", "10", "4"));

        assertThat(result.average()).isEqualTo(14.0 / 3);
        assertThat(result.recommended()).isEqualTo("5");
    }

    @Test
    void powersStartAtZero() {
        PokerVotingResult result = usecase.execute(command(PokerScale.POWERS, "0", "2", "8"));

        assertThat(result.average()).isEqualTo(10.0 / 3);
        assertThat(result.recommended()).isEqualTo("4");
    }

    @Test
    void tshirtsRecommendNearestSizeAndSkipAverage() {
        PokerVotingResult exact = usecase.execute(command(PokerScale.TSHIRTS, "S", "L", "?", "coffee"));
        assertThat(exact.average()).isNull();
        assertThat(exact.recommended()).isEqualTo("M");

        PokerVotingResult tie = usecase.execute(command(PokerScale.TSHIRTS, "M", "L"));
        assertThat(tie.recommended()).isEqualTo("L");
    }

    @Test
    void onlyBreaksLeaveNoResult() {
        PokerVotingResult result = usecase.execute(command(PokerScale.SEQUENTIAL, "?", "coffee"));

        assertThat(result.average()).isNull();
        assertThat(result.recommended()).isNull();
        verify(port, org.mockito.Mockito.atLeastOnce()).save(any(PokerVotingUpsert.class));
    }

    private static RecordPokerVotingCommand command(PokerScale scale, String... values) {
        List<PokerVoteRecord> votes = java.util.Arrays.stream(values)
                .map(value -> new PokerVoteRecord(UUID.randomUUID(), value))
                .toList();
        return RecordPokerVotingCommand.builder()
                .roomId(UUID.randomUUID())
                .roomName("room")
                .taskTitle("task")
                .scale(scale)
                .votes(votes)
                .build();
    }
}
