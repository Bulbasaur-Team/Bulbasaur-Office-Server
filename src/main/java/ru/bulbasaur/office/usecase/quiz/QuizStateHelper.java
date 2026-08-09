package ru.bulbasaur.office.usecase.quiz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.bulbasaur.office.domain.model.QuizAttempt;
import ru.bulbasaur.office.domain.model.QuizPlayerState;
import ru.bulbasaur.office.domain.model.QuizQuestion;
import ru.bulbasaur.office.usecase.GetBulbaCoinBalanceUsecase;
import ru.bulbasaur.office.usecase.port.out.QuizRepositoryPort;
import ru.bulbasaur.office.usecase.quiz.dto.QuizViews;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class QuizStateHelper {

    private final QuizRepositoryPort quiz;
    private final GetBulbaCoinBalanceUsecase balance;

    public QuizPlayerState loadWithRegen(UUID playerId, Instant now) {
        QuizPlayerState state = quiz.getOrCreateState(playerId);
        QuizConstants.RegenResult regen = QuizConstants.applyRegen(
                state.getEnergy(), state.getEnergyUpdatedAt(), now);
        if (regen.energy() != state.getEnergy()
                || !regen.energyUpdatedAt().equals(state.getEnergyUpdatedAt())) {
            state.setEnergy(regen.energy());
            state.setEnergyUpdatedAt(regen.energyUpdatedAt());
            quiz.saveState(state);
        }
        return state;
    }

    public QuizViews.StateView toView(QuizPlayerState state, UUID playerId) {
        return new QuizViews.StateView(
                state.getLevel(),
                state.getEnergy(),
                QuizConstants.MAX_ENERGY,
                QuizConstants.nextEnergyAt(state.getEnergy(), state.getEnergyUpdatedAt()),
                state.getBoosterReroll(),
                state.getBoosterFifty(),
                state.isPendingChest(),
                balance.execute(playerId),
                QuizConstants.ENERGY_PRICE,
                QuizConstants.REROLL_PRICE,
                QuizConstants.FIFTY_PRICE
        );
    }

    public QuizViews.QuestionView toQuestionView(QuizQuestion q, List<Integer> masked) {
        List<Integer> safe = masked == null ? List.of() : List.copyOf(masked);
        return new QuizViews.QuestionView(q.getText(), List.copyOf(q.getOptions()), safe);
    }

    public UUID currentQuestionId(QuizAttempt attempt) {
        List<UUID> ids = attempt.getQuestionIds();
        int idx = attempt.getCurrentIndex();
        if (ids == null || idx < 0 || idx >= ids.size()) {
            throw new IllegalArgumentException("Попытка повреждена");
        }
        return ids.get(idx);
    }

    public QuizQuestion requireQuestion(UUID id) {
        return quiz.findQuestion(id)
                .orElseThrow(() -> new IllegalArgumentException("Вопрос не найден"));
    }

    public List<UUID> pickRandomQuestionIds(String topicCode, int count) {
        List<QuizQuestion> pool = new ArrayList<>(quiz.questionsOfTopic(topicCode));
        if (pool.size() < count) {
            throw new IllegalArgumentException("В теме недостаточно вопросов");
        }
        Collections.shuffle(pool);
        return pool.stream().limit(count).map(QuizQuestion::getId).toList();
    }
}
