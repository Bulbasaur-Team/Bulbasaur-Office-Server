package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.domain.model.BulbaCoinKind;
import ru.bulbasaur.office.usecase.dto.PresentationClaudeView;
import ru.bulbasaur.office.usecase.port.out.BulbaCoinRepositoryPort;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetPresentationProgressUsecase {

    private final BulbaCoinRepositoryPort coins;

    @Transactional(readOnly = true)
    public PresentationClaudeView execute(UUID playerId) {
        boolean paid = coins.existsLedger(playerId, BulbaCoinKind.PRESENTATION_CLAUDE, PayPresentationClaudeUsecase.REF);
        return new PresentationClaudeView(paid, coins.balanceOf(playerId));
    }
}
