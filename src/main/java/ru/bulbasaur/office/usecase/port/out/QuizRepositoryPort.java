package ru.bulbasaur.office.usecase.port.out;

import ru.bulbasaur.office.domain.model.QuizAttempt;
import ru.bulbasaur.office.domain.model.QuizAttemptStatus;
import ru.bulbasaur.office.domain.model.QuizPlayerState;
import ru.bulbasaur.office.domain.model.QuizQuestion;
import ru.bulbasaur.office.domain.model.QuizTopic;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizRepositoryPort {

    List<QuizTopic> topics();

    Optional<QuizTopic> findTopic(String code);

    List<QuizQuestion> questionsOfTopic(String topicCode);

    List<QuizQuestion> questionsOfTopicExcluding(String topicCode, Collection<UUID> excludeIds);

    Optional<QuizQuestion> findQuestion(UUID id);

    QuizPlayerState getOrCreateState(UUID playerId);

    void saveState(QuizPlayerState state);

    QuizAttempt saveAttempt(QuizAttempt attempt);

    Optional<QuizAttempt> findAttempt(UUID id);

    List<QuizAttempt> findAttempts(UUID playerId, QuizAttemptStatus status);
}
