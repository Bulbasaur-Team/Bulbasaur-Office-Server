package ru.bulbasaur.office.usecase.quiz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.domain.model.QuizAttempt;
import ru.bulbasaur.office.domain.model.QuizAttemptStatus;
import ru.bulbasaur.office.domain.model.QuizBooster;
import ru.bulbasaur.office.domain.model.QuizPlayerState;
import ru.bulbasaur.office.domain.model.QuizQuestion;
import ru.bulbasaur.office.domain.model.QuizTopic;
import ru.bulbasaur.office.usecase.port.out.QuizRepositoryPort;
import ru.bulbasaur.office.usecase.quiz.dto.QuizViews;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class UseQuizBoosterUsecase {

    private final QuizRepositoryPort quiz;
    private final QuizStateHelper helper;

    @Transactional
    public QuizViews.AttemptView execute(UUID playerId, UUID attemptId, QuizBooster booster) {
        Instant now = Instant.now();
        QuizAttempt attempt = requireActive(playerId, attemptId);
        QuizTopic topic = quiz.findTopic(attempt.getTopicCode())
                .orElseThrow(() -> new IllegalArgumentException("Тема не найдена"));
        QuizPlayerState state = helper.loadWithRegen(playerId, now);

        if (now.isAfter(attempt.getQuestionDeadline().plus(QuizConstants.ANSWER_GRACE))) {
            attempt.setStatus(QuizAttemptStatus.LOST);
            quiz.saveAttempt(attempt);
            throw new IllegalArgumentException("Время на ответ истекло");
        }

        return switch (booster) {
            case REROLL -> useRerollBooster(playerId, attempt, topic, state, now);
            case FIFTY -> useFiftyFiftyBooster(playerId, attempt, topic, state);
        };
    }

    private QuizViews.AttemptView useRerollBooster(
            UUID playerId,
            QuizAttempt attempt,
            QuizTopic topic,
            QuizPlayerState state,
            Instant now
    ) {
        if (state.getBoosterReroll() < 1) {
            throw new IllegalArgumentException("Нет бустера «Другой вопрос»");
        }
        Set<UUID> exclude = new HashSet<>(attempt.getQuestionIds());
        List<QuizQuestion> pool = quiz.questionsOfTopicExcluding(attempt.getTopicCode(), exclude);
        if (pool.isEmpty()) {
            pool = quiz.questionsOfTopic(attempt.getTopicCode()).stream()
                    .filter(q -> !q.getId().equals(helper.currentQuestionId(attempt)))
                    .toList();
        }
        if (pool.isEmpty()) {
            throw new IllegalArgumentException("Нет другого вопроса в этой теме");
        }
        QuizQuestion replacement = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        List<UUID> ids = new ArrayList<>(attempt.getQuestionIds());
        ids.set(attempt.getCurrentIndex(), replacement.getId());
        attempt.setQuestionIds(ids);
        attempt.setFiftyMasked(null);
        attempt.setQuestionDeadline(now.plus(QuizConstants.ANSWER_TIME));
        quiz.saveAttempt(attempt);

        state.setBoosterReroll(state.getBoosterReroll() - 1);
        quiz.saveState(state);

        return buildView(playerId, attempt, topic, state, replacement, List.of());
    }

    private QuizViews.AttemptView useFiftyFiftyBooster(
            UUID playerId,
            QuizAttempt attempt,
            QuizTopic topic,
            QuizPlayerState state
    ) {
        if (state.getBoosterFifty() < 1) {
            throw new IllegalArgumentException("Нет бустера «50/50»");
        }
        if (attempt.getFiftyMasked() != null && !attempt.getFiftyMasked().isEmpty()) {
            throw new IllegalArgumentException("50/50 уже использован на этом вопросе");
        }
        QuizQuestion q = helper.requireQuestion(helper.currentQuestionId(attempt));
        List<Integer> wrong = new ArrayList<>();
        for (int i = 0; i < q.getOptions().size(); i++) {
            if (i != q.getCorrectIndex()) {
                wrong.add(i);
            }
        }
        Collections.shuffle(wrong);
        List<Integer> masked = wrong.stream().limit(2).sorted().toList();
        attempt.setFiftyMasked(masked);
        quiz.saveAttempt(attempt);

        state.setBoosterFifty(state.getBoosterFifty() - 1);
        quiz.saveState(state);

        return buildView(playerId, attempt, topic, state, q, masked);
    }

    private QuizViews.AttemptView buildView(
            UUID playerId,
            QuizAttempt attempt,
            QuizTopic topic,
            QuizPlayerState state,
            QuizQuestion question,
            List<Integer> masked
    ) {
        return QuizViews.AttemptView.builder()
                .attemptId(attempt.getId())
                .topicCode(topic.getCode())
                .topicName(topic.getName())
                .status(attempt.getStatus().name())
                .currentIndex(attempt.getCurrentIndex())
                .totalQuestions(QuizConstants.QUESTIONS_PER_ATTEMPT)
                .question(helper.toQuestionView(question, masked))
                .deadlineAt(attempt.getQuestionDeadline())
                .correct(false)
                .state(helper.toView(state, playerId))
                .build();
    }

    private QuizAttempt requireActive(UUID playerId, UUID attemptId) {
        QuizAttempt attempt = quiz.findAttempt(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Попытка не найдена"));
        if (!attempt.getPlayerId().equals(playerId)) {
            throw new IllegalArgumentException("Попытка не найдена");
        }
        if (attempt.getStatus() != QuizAttemptStatus.ACTIVE) {
            throw new IllegalArgumentException("Попытка уже завершена");
        }
        return attempt;
    }
}
