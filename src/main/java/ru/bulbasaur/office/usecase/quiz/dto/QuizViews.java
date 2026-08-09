package ru.bulbasaur.office.usecase.quiz.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class QuizViews {

    private QuizViews() {
    }

    public record TopicView(String code, String name) {
    }

    public record QuestionView(String text, List<String> options, List<Integer> maskedIndices) {
    }

    public record StateView(
            int level,
            int energy,
            int maxEnergy,
            Instant nextEnergyAt,
            int boosterReroll,
            int boosterFifty,
            boolean pendingChest,
            long bulbaCoinBalance,
            int energyPrice,
            int rerollPrice,
            int fiftyPrice
    ) {
    }

    @Builder
    public record AttemptView(
            UUID attemptId,
            String topicCode,
            String topicName,
            String status,
            int currentIndex,
            int totalQuestions,
            QuestionView question,
            Instant deadlineAt,
            boolean correct,
            StateView state
    ) {
    }

    @Builder
    public record ChestRewardView(
            ChestItemView item,
            boolean duplicateSold,
            Long sellRefund,
            long coins,
            int energy,
            long bulbaCoinBalance,
            StateView state
    ) {
    }

    public record ChestItemView(String code, String name, long price) {
    }
}
