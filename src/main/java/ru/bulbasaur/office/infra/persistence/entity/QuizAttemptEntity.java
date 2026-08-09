package ru.bulbasaur.office.infra.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "quiz_attempt")
@Getter
@Setter
@NoArgsConstructor
public class QuizAttemptEntity {

    @Id
    private UUID id;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "topic_code", nullable = false)
    private String topicCode;

    @Column(nullable = false, length = 16)
    private String status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "question_ids", nullable = false, columnDefinition = "jsonb")
    private List<UUID> questionIds = new ArrayList<>();

    @Column(name = "current_index", nullable = false)
    private int currentIndex;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fifty_masked", columnDefinition = "jsonb")
    private List<Integer> fiftyMasked;

    @Column(name = "question_deadline", nullable = false)
    private Instant questionDeadline;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
