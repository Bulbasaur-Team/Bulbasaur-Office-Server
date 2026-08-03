package ru.bulbasaur.office.infra.persistence.connector;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.domain.model.BulbaCoinKind;
import ru.bulbasaur.office.infra.persistence.entity.BulbaCoinLedgerEntity;
import ru.bulbasaur.office.infra.persistence.repository.BulbaCoinLedgerJpaRepository;
import ru.bulbasaur.office.infra.persistence.repository.PlayerJpaRepository;
import ru.bulbasaur.office.usecase.dto.BulbaCoinTransactionView;
import ru.bulbasaur.office.usecase.port.out.BulbaCoinRepositoryPort;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BulbaCoinConnector implements BulbaCoinRepositoryPort {

    private final BulbaCoinLedgerJpaRepository ledger;
    private final PlayerJpaRepository players;

    @Override
    @Transactional
    public boolean insertLedger(UUID playerId, long amount, BulbaCoinKind kind, String ref, String title) {
        return ledger.insertIfAbsent(playerId, amount, kind.name(), ref, title) > 0;
    }

    @Override
    @Transactional
    public void addBalance(UUID playerId, long delta) {
        players.addBalance(playerId, delta);
    }

    @Override
    @Transactional
    public boolean subtractBalance(UUID playerId, long amount) {
        return players.subtractBalanceIfEnough(playerId, amount) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public long balanceOf(UUID playerId) {
        return players.findBalance(playerId).orElse(0L);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BulbaCoinTransactionView> history(UUID playerId, Instant before, int limit) {
        List<BulbaCoinLedgerEntity> rows = before == null
                ? ledger.findHistory(playerId, PageRequest.of(0, limit))
                : ledger.findHistoryBefore(playerId, before, PageRequest.of(0, limit));
        return rows.stream()
                .map(this::toView)
                .toList();
    }

    private BulbaCoinTransactionView toView(BulbaCoinLedgerEntity e) {
        return new BulbaCoinTransactionView(e.getId(), e.getAmount(), e.getKind(), e.getTitle(), e.getCreatedAt());
    }
}
