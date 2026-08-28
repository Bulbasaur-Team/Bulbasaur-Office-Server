package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.domain.model.BulbaCoinKind;
import ru.bulbasaur.office.domain.model.QuestCode;
import ru.bulbasaur.office.domain.model.QuestStatus;
import ru.bulbasaur.office.usecase.dto.PresentationClaudeView;
import ru.bulbasaur.office.usecase.port.out.BulbaCoinRepositoryPort;
import ru.bulbasaur.office.usecase.port.out.QuestRepositoryPort;

import java.util.UUID;

/** Списать 10000 BC за презентацию Claude. Идемпотентно. */
@Service
@RequiredArgsConstructor
public class PayPresentationClaudeUsecase {

    public static final long PRICE_BC = 10_000L;
    public static final String REF = "presentation:claude";
    public static final String TITLE = "Презентация Claude";

    private final QuestRepositoryPort quests;
    private final BulbaCoinRepositoryPort coins;
    private final DebitBulbaCoinsUsecase debit;

    @Transactional
    public PresentationClaudeView execute(UUID playerId) {
        QuestStatus status = quests.findStatus(playerId, QuestCode.PRESENTATION).orElse(QuestStatus.LOCKED);
        if (status != QuestStatus.IN_PROGRESS && status != QuestStatus.COMPLETED) {
            throw new IllegalArgumentException("Сначала нужно взять квест «Сборка презентации»");
        }
        if (coins.existsLedger(playerId, BulbaCoinKind.PRESENTATION_CLAUDE, REF)) {
            return new PresentationClaudeView(true, coins.balanceOf(playerId));
        }
        debit.execute(playerId, PRICE_BC, BulbaCoinKind.PRESENTATION_CLAUDE, REF, TITLE);
        return new PresentationClaudeView(true, coins.balanceOf(playerId));
    }
}
