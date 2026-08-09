package ru.bulbasaur.office.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class QuizAttempt {

    private UUID id;
    private UUID playerId;
    private String topicCode;
    private QuizAttemptStatus status;
    private List<UUID> questionIds;
    private int currentIndex;
    private List<Integer> fiftyMasked;
    private Instant questionDeadline;
    private Instant createdAt;
}
