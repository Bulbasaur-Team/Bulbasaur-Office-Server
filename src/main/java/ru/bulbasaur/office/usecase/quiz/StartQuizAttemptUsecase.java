package ru.bulbasaur.office.usecase.quiz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.domain.model.QuestCode;
import ru.bulbasaur.office.domain.model.QuestStatus;
import ru.bulbasaur.office.domain.model.QuizAttempt;
import ru.bulbasaur.office.domain.model.QuizAttemptStatus;
import ru.bulbasaur.office.domain.model.QuizPlayerState;
import ru.bulbasaur.office.domain.model.QuizQuestion;
import ru.bulbasaur.office.domain.model.QuizTopic;
import ru.bulbasaur.office.usecase.port.out.QuestRepositoryPort;
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
    private final QuestRepositoryPort quests;

    @Transactional
    public QuizViews.AttemptView execute(UUID playerId, String topicCode) {
        Instant now = Instant.now();
        QuizTopic topic = quiz.findTopic(topicCode)
                .orElseThrow(() -> new IllegalArgumentException("Тема не найдена"));
        closeActiveAttempts(playerId);
        if (topic.isStory()) {
            return startStory(playerId, topic, now);
        }
        requireOfferedTopic(playerId, topicCode);
        QuizPlayerState state = spendEnergy(playerId, now);
        QuizAttempt attempt = createAttempt(playerId, topicCode, QuizConstants.QUESTIONS_PER_ATTEMPT, now);
        QuizQuestion question = helper.requireQuestion(attempt.getQuestionIds().getFirst());
        return toView(attempt, topic, question, state, playerId);
    }

    private QuizViews.AttemptView startStory(UUID playerId, QuizTopic topic, Instant now) {
        if (!StoryQuizConstants.QUANTUM_TOPIC.equals(topic.getCode())) {
            throw new IllegalArgumentException("Неизвестный сюжетный тест");
        }
        QuestStatus status = quests.findStatus(playerId, QuestCode.QUANTUM).orElse(QuestStatus.LOCKED);
        if (status != QuestStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Сначала поговори с техлидом — тест ещё недоступен");
        }
        QuizPlayerState state = helper.loadWithRegen(playerId, now);
        QuizAttempt attempt = createAttempt(playerId, topic.getCode(), StoryQuizConstants.QUESTIONS, now);
        QuizQuestion question = helper.requireQuestion(attempt.getQuestionIds().getFirst());
        return toView(attempt, topic, question, state, playerId);
    }

    private void requireOfferedTopic(UUID playerId, String topicCode) {
        boolean offered = getTopics.execute(playerId).stream()
                .anyMatch(topic -> topic.code().equals(topicCode));
        if (!offered) {
            throw new IllegalArgumentException("Эта тема недоступна на текущем уровне");
        }
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

    private QuizAttempt createAttempt(UUID playerId, String topicCode, int count, Instant now) {
        List<UUID> questionIds = new ArrayList<>(helper.pickRandomQuestionIds(topicCode, count));
        QuizAttempt attempt = QuizAttempt.builder()
                .id(UUID.randomUUID())
                .playerId(playerId)
                .topicCode(topicCode)
                .status(QuizAttemptStatus.ACTIVE)
                .questionIds(questionIds)
                .currentIndex(0)
                .questionDeadline(now.plus(QuizConstants.ANSWER_TIME))
                .createdAt(now)
                .correctCount(0)
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
                .totalQuestions(attempt.getQuestionIds().size())
                .correctCount(0)
                .story(topic.isStory())
                .question(helper.toQuestionView(question, List.of()))
                .deadlineAt(attempt.getQuestionDeadline())
                .correct(false)
                .state(helper.toView(state, playerId))
                .build();
    }
}
