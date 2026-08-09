package ru.bulbasaur.office.infra.persistence.connector;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.domain.model.QuizAttempt;
import ru.bulbasaur.office.domain.model.QuizAttemptStatus;
import ru.bulbasaur.office.domain.model.QuizPlayerState;
import ru.bulbasaur.office.domain.model.QuizQuestion;
import ru.bulbasaur.office.domain.model.QuizTopic;
import ru.bulbasaur.office.infra.persistence.entity.QuizAttemptEntity;
import ru.bulbasaur.office.infra.persistence.entity.QuizPlayerStateEntity;
import ru.bulbasaur.office.infra.persistence.entity.QuizQuestionEntity;
import ru.bulbasaur.office.infra.persistence.entity.QuizTopicEntity;
import ru.bulbasaur.office.infra.persistence.repository.QuizAttemptJpaRepository;
import ru.bulbasaur.office.infra.persistence.repository.QuizPlayerStateJpaRepository;
import ru.bulbasaur.office.infra.persistence.repository.QuizQuestionJpaRepository;
import ru.bulbasaur.office.infra.persistence.repository.QuizTopicJpaRepository;
import ru.bulbasaur.office.usecase.port.out.QuizRepositoryPort;
import ru.bulbasaur.office.usecase.quiz.QuizConstants;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class QuizConnector implements QuizRepositoryPort {

    private final QuizTopicJpaRepository topics;
    private final QuizQuestionJpaRepository questions;
    private final QuizPlayerStateJpaRepository states;
    private final QuizAttemptJpaRepository attempts;

    @Override
    @Transactional(readOnly = true)
    public List<QuizTopic> topics() {
        return topics.findAllByOrderBySortOrderAsc().stream()
                .map(QuizConnector::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QuizTopic> findTopic(String code) {
        return topics.findById(code).map(QuizConnector::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizQuestion> questionsOfTopic(String topicCode) {
        return questions.findByTopicCode(topicCode).stream()
                .map(QuizConnector::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizQuestion> questionsOfTopicExcluding(String topicCode, Collection<UUID> excludeIds) {
        if (excludeIds == null || excludeIds.isEmpty()) {
            return questionsOfTopic(topicCode);
        }
        return questions.findByTopicCodeAndIdNotIn(topicCode, excludeIds).stream()
                .map(QuizConnector::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QuizQuestion> findQuestion(UUID id) {
        return questions.findById(id).map(QuizConnector::toDomain);
    }

    @Override
    @Transactional
    public QuizPlayerState getOrCreateState(UUID playerId) {
        QuizPlayerStateEntity entity = states.findById(playerId).orElseGet(() -> {
            QuizPlayerStateEntity created = new QuizPlayerStateEntity();
            created.setPlayerId(playerId);
            created.setLevel(0);
            created.setEnergy(QuizConstants.MAX_ENERGY);
            created.setEnergyUpdatedAt(Instant.now());
            created.setBoosterReroll(0);
            created.setBoosterFifty(0);
            created.setPendingChest(false);
            created.setChestsOpened(0);
            return states.save(created);
        });
        return toDomain(entity);
    }

    @Override
    @Transactional
    public void saveState(QuizPlayerState state) {
        states.save(toEntity(state));
    }

    @Override
    @Transactional
    public QuizAttempt saveAttempt(QuizAttempt attempt) {
        return toDomain(attempts.save(toEntity(attempt)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QuizAttempt> findAttempt(UUID id) {
        return attempts.findById(id).map(QuizConnector::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizAttempt> findAttempts(UUID playerId, QuizAttemptStatus status) {
        return attempts.findByPlayerIdAndStatus(playerId, status.name()).stream()
                .map(QuizConnector::toDomain)
                .toList();
    }

    private static QuizTopic toDomain(QuizTopicEntity entity) {
        return QuizTopic.builder()
                .code(entity.getCode())
                .name(entity.getName())
                .sortOrder(entity.getSortOrder())
                .build();
    }

    private static QuizQuestion toDomain(QuizQuestionEntity entity) {
        return QuizQuestion.builder()
                .id(entity.getId())
                .topicCode(entity.getTopicCode())
                .text(entity.getText())
                .options(List.copyOf(entity.getOptions()))
                .correctIndex(entity.getCorrectIndex())
                .build();
    }

    private static QuizPlayerState toDomain(QuizPlayerStateEntity entity) {
        return QuizPlayerState.builder()
                .playerId(entity.getPlayerId())
                .level(entity.getLevel())
                .energy(entity.getEnergy())
                .energyUpdatedAt(entity.getEnergyUpdatedAt())
                .boosterReroll(entity.getBoosterReroll())
                .boosterFifty(entity.getBoosterFifty())
                .pendingChest(entity.isPendingChest())
                .chestsOpened(entity.getChestsOpened())
                .build();
    }

    private static QuizPlayerStateEntity toEntity(QuizPlayerState state) {
        QuizPlayerStateEntity entity = new QuizPlayerStateEntity();
        entity.setPlayerId(state.getPlayerId());
        entity.setLevel(state.getLevel());
        entity.setEnergy(state.getEnergy());
        entity.setEnergyUpdatedAt(state.getEnergyUpdatedAt());
        entity.setBoosterReroll(state.getBoosterReroll());
        entity.setBoosterFifty(state.getBoosterFifty());
        entity.setPendingChest(state.isPendingChest());
        entity.setChestsOpened(state.getChestsOpened());
        return entity;
    }

    private static QuizAttempt toDomain(QuizAttemptEntity entity) {
        return QuizAttempt.builder()
                .id(entity.getId())
                .playerId(entity.getPlayerId())
                .topicCode(entity.getTopicCode())
                .status(QuizAttemptStatus.valueOf(entity.getStatus()))
                .questionIds(List.copyOf(entity.getQuestionIds()))
                .currentIndex(entity.getCurrentIndex())
                .fiftyMasked(entity.getFiftyMasked() == null ? null : List.copyOf(entity.getFiftyMasked()))
                .questionDeadline(entity.getQuestionDeadline())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private static QuizAttemptEntity toEntity(QuizAttempt attempt) {
        QuizAttemptEntity entity = new QuizAttemptEntity();
        entity.setId(attempt.getId());
        entity.setPlayerId(attempt.getPlayerId());
        entity.setTopicCode(attempt.getTopicCode());
        entity.setStatus(attempt.getStatus().name());
        entity.setQuestionIds(List.copyOf(attempt.getQuestionIds()));
        entity.setCurrentIndex(attempt.getCurrentIndex());
        entity.setFiftyMasked(attempt.getFiftyMasked() == null ? null : List.copyOf(attempt.getFiftyMasked()));
        entity.setQuestionDeadline(attempt.getQuestionDeadline());
        entity.setCreatedAt(attempt.getCreatedAt());
        return entity;
    }
}
