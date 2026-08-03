package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.usecase.dto.BulbaCoinHistoryView;
import ru.bulbasaur.office.usecase.port.out.BulbaCoinRepositoryPort;

import java.time.Instant;
import java.util.UUID;

/** История транзакций BC с текущим балансом. */
@Service
@RequiredArgsConstructor
public class ListBulbaCoinTransactionsUsecase {

    private final BulbaCoinRepositoryPort coins;

    @Transactional(readOnly = true)
    public BulbaCoinHistoryView execute(UUID playerId, Instant before, int limit) {
        int capped = Math.min(Math.max(limit, 1), 100);
        return new BulbaCoinHistoryView(
                coins.balanceOf(playerId),
                coins.history(playerId, before, capped));
    }
}
