package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.domain.model.BulbaCoinKind;
import ru.bulbasaur.office.usecase.port.out.BulbaCoinRepositoryPort;

import java.util.UUID;

/** Начислить BC; идемпотентно по (kind, ref). true — начислено сейчас. */
@Service
@RequiredArgsConstructor
public class CreditBulbaCoinsUsecase {

    private final BulbaCoinRepositoryPort coins;

    @Transactional
    public boolean execute(UUID playerId, long amount, BulbaCoinKind kind, String ref, String title) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Сумма начисления должна быть положительной");
        }
        if (!coins.insertLedger(playerId, amount, kind, ref, title)) {
            return false;
        }
        coins.addBalance(playerId, amount);
        return true;
    }
}
