package ru.bulbasaur.office.usecase.quiz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.domain.model.BulbaCoinKind;
import ru.bulbasaur.office.domain.model.QuizPlayerState;
import ru.bulbasaur.office.usecase.DebitBulbaCoinsUsecase;
import ru.bulbasaur.office.usecase.port.out.QuizRepositoryPort;
import ru.bulbasaur.office.usecase.quiz.dto.QuizViews;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BuyQuizEnergyUsecase {

    private final QuizRepositoryPort quiz;
    private final QuizStateHelper helper;
    private final DebitBulbaCoinsUsecase debit;

    @Transactional
    public QuizViews.StateView execute(UUID playerId) {
        Instant now = Instant.now();
        QuizPlayerState state = helper.loadWithRegen(playerId, now);
        if (state.getEnergy() >= QuizConstants.MAX_ENERGY) {
            throw new IllegalArgumentException("Энергия уже полная");
        }
        UUID ref = UUID.randomUUID();
        debit.execute(
                playerId,
                QuizConstants.ENERGY_PRICE,
                BulbaCoinKind.QUIZ_ENERGY_BUY,
                "quiz:energy:" + ref,
                "Bulba Quiz: энергия");
        state.setEnergy(state.getEnergy() + 1);
        if (state.getEnergy() >= QuizConstants.MAX_ENERGY) {
            state.setEnergyUpdatedAt(now);
        }
        quiz.saveState(state);
        return helper.toView(state, playerId);
    }
}
