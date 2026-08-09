package ru.bulbasaur.office.usecase.quiz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.domain.model.GameId;
import ru.bulbasaur.office.domain.model.QuizAttempt;
import ru.bulbasaur.office.domain.model.QuizAttemptStatus;
import ru.bulbasaur.office.domain.model.QuizPlayerState;
import ru.bulbasaur.office.domain.model.QuizQuestion;
import ru.bulbasaur.office.domain.model.QuizTopic;
import ru.bulbasaur.office.usecase.EventLogService;
import ru.bulbasaur.office.usecase.SubmitScoreUsecase;
import ru.bulbasaur.office.usecase.port.out.QuizRepositoryPort;
import ru.bulbasaur.office.usecase.quiz.dto.QuizViews;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnswerQuizAttemptUsecase {

    private static final int LB_LIMIT = 20;

    private final QuizRepositoryPort quiz;
    private final QuizStateHelper helper;
    private final SubmitScoreUsecase submitScore;
    private final EventLogService eventLog;

    @Transactional
    public QuizViews.AttemptView execute(UUID playerId, String login, UUID attemptId, int optionIndex) {
        Instant now = Instant.now();
        QuizAttempt attempt = requireActive(playerId, attemptId);
        QuizTopic topic = quiz.findTopic(attempt.getTopicCode())
                .orElseThrow(() -> new IllegalArgumentException("Тема не найдена"));
        QuizPlayerState state = helper.loadWithRegen(playerId, now);

        if (answerTimedOut(attempt, optionIndex, now)) {
            return fail(attempt, topic, state, playerId);
        }

        QuizQuestion question = helper.requireQuestion(helper.currentQuestionId(attempt));
        validateOption(attempt, question, optionIndex);
        if (optionIndex != question.getCorrectIndex()) {
            return fail(attempt, topic, state, playerId);
        }

        int nextIndex = attempt.getCurrentIndex() + 1;
        if (nextIndex >= attempt.getQuestionIds().size()) {
            return completeLevel(playerId, login, attempt, topic, state, nextIndex);
        }
        return advanceToNextQuestion(playerId, attempt, topic, state, nextIndex, now);
    }

    private QuizViews.AttemptView advanceToNextQuestion(
            UUID playerId,
            QuizAttempt attempt,
            QuizTopic topic,
            QuizPlayerState state,
            int nextIndex,
            Instant now
    ) {
        attempt.setCurrentIndex(nextIndex);
        attempt.setFiftyMasked(null);
        // Клиент 800 мс показывает зелёный правильный ответ до следующего вопроса.
        // Добавляем это время, чтобы после анимации у игрока осталось полных 20 секунд.
        attempt.setQuestionDeadline(now
                .plus(QuizConstants.CORRECT_FEEDBACK_TIME)
                .plus(QuizConstants.ANSWER_TIME));
        quiz.saveAttempt(attempt);

        QuizQuestion next = helper.requireQuestion(attempt.getQuestionIds().get(nextIndex));
        return buildView(playerId, attempt, topic, state, next, true);
    }

    private QuizViews.AttemptView completeLevel(
            UUID playerId,
            String login,
            QuizAttempt attempt,
            QuizTopic topic,
            QuizPlayerState state,
            int nextIndex
    ) {
        attempt.setStatus(QuizAttemptStatus.WON);
        attempt.setCurrentIndex(nextIndex);
        attempt.setFiftyMasked(null);
        quiz.saveAttempt(attempt);

        state.setLevel(state.getLevel() + 1);
        if (state.getLevel() % QuizConstants.CHEST_EVERY == 0) {
            state.setPendingChest(true);
        }
        quiz.saveState(state);
        submitScore.execute(playerId, login, GameId.BULBA_QUIZ, state.getLevel(), LB_LIMIT);
        eventLog.quizLevelReached(login, state.getLevel());
        return buildView(playerId, attempt, topic, state, null, true);
    }

    private QuizViews.AttemptView fail(
            QuizAttempt attempt,
            QuizTopic topic,
            QuizPlayerState state,
            UUID playerId
    ) {
        attempt.setStatus(QuizAttemptStatus.LOST);
        quiz.saveAttempt(attempt);
        return buildView(playerId, attempt, topic, state, null, false);
    }

    private QuizViews.AttemptView buildView(
            UUID playerId,
            QuizAttempt attempt,
            QuizTopic topic,
            QuizPlayerState state,
            QuizQuestion question,
            boolean correct
    ) {
        return QuizViews.AttemptView.builder()
                .attemptId(attempt.getId())
                .topicCode(topic.getCode())
                .topicName(topic.getName())
                .status(attempt.getStatus().name())
                .currentIndex(attempt.getCurrentIndex())
                .totalQuestions(QuizConstants.QUESTIONS_PER_ATTEMPT)
                .question(question == null ? null : helper.toQuestionView(question, List.of()))
                .deadlineAt(attempt.getQuestionDeadline())
                .correct(correct)
                .state(helper.toView(state, playerId))
                .build();
    }

    private static boolean answerTimedOut(QuizAttempt attempt, int optionIndex, Instant now) {
        return optionIndex < 0
                || now.isAfter(attempt.getQuestionDeadline().plus(QuizConstants.ANSWER_GRACE));
    }

    private static void validateOption(QuizAttempt attempt, QuizQuestion question, int optionIndex) {
        List<Integer> masked = attempt.getFiftyMasked();
        if (masked != null && masked.contains(optionIndex)) {
            throw new IllegalArgumentException("Этот вариант уже убран бустером");
        }
        if (optionIndex >= question.getOptions().size()) {
            throw new IllegalArgumentException("Неверный вариант ответа");
        }
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
