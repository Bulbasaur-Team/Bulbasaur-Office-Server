package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.domain.model.BulbaCoinKind;
import ru.bulbasaur.office.usecase.port.out.BulbaCoinRepositoryPort;

import java.util.UUID;

/** Платная подсказка Bulba Guess. Слово выбирает клиент, сервер только списывает монеты. */
@Service
@RequiredArgsConstructor
public class BuyGuessHintUsecase {

    public static final long PRICE = 100;

    private final DebitBulbaCoinsUsecase debit;
    private final BulbaCoinRepositoryPort coins;

    @Transactional
    public long execute(UUID playerId) {
        debit.execute(
                playerId,
                PRICE,
                BulbaCoinKind.GUESS_HINT_BUY,
                "guess:hint:" + UUID.randomUUID(),
                "Bulba Guess: подсказка");
        return coins.balanceOf(playerId);
    }
}
