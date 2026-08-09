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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "quiz_questions")
@Getter
@Setter
@NoArgsConstructor
public class QuizQuestionEntity {

    @Id
    private UUID id;

    @Column(name = "topic_code", nullable = false)
    private String topicCode;

    @Column(nullable = false, length = 500)
    private String text;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> options = new ArrayList<>();

    @Column(name = "correct_index", nullable = false)
    private int correctIndex;
}
