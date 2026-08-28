package ru.bulbasaur.office.usecase.port.out;

import ru.bulbasaur.office.domain.model.BulbaCoinKind;
import ru.bulbasaur.office.usecase.dto.BulbaCoinTransactionView;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface BulbaCoinRepositoryPort {

    /** Вставить запись в журнал; true — вставлена, false — дубликат. */
    boolean insertLedger(UUID playerId, long amount, BulbaCoinKind kind, String ref, String title);

    void addBalance(UUID playerId, long delta);

    /** Списать сумму, если хватает баланса; true — списано. */
    boolean subtractBalance(UUID playerId, long amount);

    long balanceOf(UUID playerId);

    boolean existsLedger(UUID playerId, BulbaCoinKind kind, String ref);

    /**
     * Удалить запись журнала и вернуть списанную сумму на баланс (для debit amount &lt; 0).
     * @return true, если запись была и удалена
     */
    boolean removeLedgerRefunding(UUID playerId, BulbaCoinKind kind, String ref);

    List<BulbaCoinTransactionView> history(UUID playerId, Instant before, int limit);
}
