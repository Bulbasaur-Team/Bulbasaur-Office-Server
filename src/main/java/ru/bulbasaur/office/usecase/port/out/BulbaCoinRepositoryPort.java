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

    List<BulbaCoinTransactionView> history(UUID playerId, Instant before, int limit);
}
