package ru.bulbasaur.office.usecase.quiz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.domain.model.BulbaCoinKind;
import ru.bulbasaur.office.domain.model.QuizBooster;
import ru.bulbasaur.office.domain.model.QuizPlayerState;
import ru.bulbasaur.office.usecase.DebitBulbaCoinsUsecase;
import ru.bulbasaur.office.usecase.port.out.QuizRepositoryPort;
import ru.bulbasaur.office.usecase.quiz.dto.QuizViews;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BuyQuizBoosterUsecase {

    private final QuizRepositoryPort quiz;
    private final QuizStateHelper helper;
    private final DebitBulbaCoinsUsecase debit;

    @Transactional
    public QuizViews.StateView execute(UUID playerId, QuizBooster booster) {
        Instant now = Instant.now();
        QuizPlayerState state = helper.loadWithRegen(playerId, now);
        int price = booster == QuizBooster.REROLL
                ? QuizConstants.REROLL_PRICE
                : QuizConstants.FIFTY_PRICE;
        String title = booster == QuizBooster.REROLL
                ? "Bulba Quiz: другой вопрос"
                : "Bulba Quiz: 50/50";
        UUID ref = UUID.randomUUID();
        debit.execute(
                playerId,
                price,
                BulbaCoinKind.QUIZ_BOOSTER_BUY,
                "quiz:booster:" + booster.code() + ":" + ref,
                title);
        if (booster == QuizBooster.REROLL) {
            state.setBoosterReroll(state.getBoosterReroll() + 1);
        } else {
            state.setBoosterFifty(state.getBoosterFifty() + 1);
        }
        quiz.saveState(state);
        return helper.toView(state, playerId);
    }
}
