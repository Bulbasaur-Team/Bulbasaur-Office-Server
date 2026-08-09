package ru.bulbasaur.office.usecase.quiz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.domain.model.QuizAttempt;
import ru.bulbasaur.office.domain.model.QuizAttemptStatus;
import ru.bulbasaur.office.domain.model.QuizPlayerState;
import ru.bulbasaur.office.domain.model.QuizQuestion;
import ru.bulbasaur.office.domain.model.QuizTopic;
import ru.bulbasaur.office.usecase.port.out.QuizRepositoryPort;
import ru.bulbasaur.office.usecase.quiz.dto.QuizViews;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StartQuizAttemptUsecase {

    private final QuizRepositoryPort quiz;
    private final QuizStateHelper helper;
    private final GetQuizTopicsUsecase getTopics;

    @Transactional
    public QuizViews.AttemptView execute(UUID playerId, String topicCode) {
        Instant now = Instant.now();
        QuizTopic topic = requireOfferedTopic(playerId, topicCode);
        closeActiveAttempts(playerId);
        QuizPlayerState state = spendEnergy(playerId, now);
        QuizAttempt attempt = createAttempt(playerId, topicCode, now);
        QuizQuestion question = helper.requireQuestion(attempt.getQuestionIds().getFirst());
        return toView(attempt, topic, question, state, playerId);
    }

    private QuizTopic requireOfferedTopic(UUID playerId, String topicCode) {
        boolean offered = getTopics.execute(playerId).stream()
                .anyMatch(topic -> topic.code().equals(topicCode));
        if (!offered) {
            throw new IllegalArgumentException("Эта тема недоступна на текущем уровне");
        }
        return quiz.findTopic(topicCode)
                .orElseThrow(() -> new IllegalArgumentException("Тема не найдена"));
    }

    private void closeActiveAttempts(UUID playerId) {
        for (QuizAttempt active : quiz.findAttempts(playerId, QuizAttemptStatus.ACTIVE)) {
            active.setStatus(QuizAttemptStatus.LOST);
            quiz.saveAttempt(active);
        }
    }

    private QuizPlayerState spendEnergy(UUID playerId, Instant now) {
        QuizPlayerState state = helper.loadWithRegen(playerId, now);
        if (state.getEnergy() < 1) {
            throw new IllegalArgumentException("Недостаточно энергии");
        }
        boolean wasFull = state.getEnergy() >= QuizConstants.MAX_ENERGY;
        state.setEnergy(state.getEnergy() - 1);
        if (wasFull) {
            state.setEnergyUpdatedAt(now);
        }
        quiz.saveState(state);
        return state;
    }

    private QuizAttempt createAttempt(UUID playerId, String topicCode, Instant now) {
        List<UUID> questionIds = new ArrayList<>(helper.pickRandomQuestionIds(
                topicCode, QuizConstants.QUESTIONS_PER_ATTEMPT));
        QuizAttempt attempt = QuizAttempt.builder()
                .id(UUID.randomUUID())
                .playerId(playerId)
                .topicCode(topicCode)
                .status(QuizAttemptStatus.ACTIVE)
                .questionIds(questionIds)
                .currentIndex(0)
                .questionDeadline(now.plus(QuizConstants.ANSWER_TIME))
                .createdAt(now)
                .build();
        return quiz.saveAttempt(attempt);
    }

    private QuizViews.AttemptView toView(
            QuizAttempt attempt,
            QuizTopic topic,
            QuizQuestion question,
            QuizPlayerState state,
            UUID playerId
    ) {
        return QuizViews.AttemptView.builder()
                .attemptId(attempt.getId())
                .topicCode(topic.getCode())
                .topicName(topic.getName())
                .status(attempt.getStatus().name())
                .currentIndex(0)
                .totalQuestions(QuizConstants.QUESTIONS_PER_ATTEMPT)
                .question(helper.toQuestionView(question, List.of()))
                .deadlineAt(attempt.getQuestionDeadline())
                .correct(false)
                .state(helper.toView(state, playerId))
                .build();
    }
}
