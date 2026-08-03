package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.domain.model.BulbaCoinKind;
import ru.bulbasaur.office.usecase.port.out.BulbaCoinRepositoryPort;

import java.util.UUID;

/** Списать BC; идемпотентно по (kind, ref). */
@Service
@RequiredArgsConstructor
public class DebitBulbaCoinsUsecase {

    private final BulbaCoinRepositoryPort coins;

    @Transactional
    public void execute(UUID playerId, long amount, BulbaCoinKind kind, String ref, String title) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Сумма списания должна быть положительной");
        }
        if (coins.balanceOf(playerId) < amount) {
            throw new IllegalArgumentException("Недостаточно Bulba Coins");
        }
        if (!coins.insertLedger(playerId, -amount, kind, ref, title)) {
            return; // уже списано
        }
        if (!coins.subtractBalance(playerId, amount)) {
            throw new IllegalArgumentException("Недостаточно Bulba Coins");
        }
    }
}
