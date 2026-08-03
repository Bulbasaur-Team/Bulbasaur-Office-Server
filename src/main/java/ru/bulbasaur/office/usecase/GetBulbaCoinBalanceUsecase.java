package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.usecase.port.out.BulbaCoinRepositoryPort;

import java.util.UUID;

/** Текущий баланс Bulba Coins игрока. */
@Service
@RequiredArgsConstructor
public class GetBulbaCoinBalanceUsecase {

    private final BulbaCoinRepositoryPort coins;

    @Transactional(readOnly = true)
    public long execute(UUID playerId) {
        return coins.balanceOf(playerId);
    }
}
