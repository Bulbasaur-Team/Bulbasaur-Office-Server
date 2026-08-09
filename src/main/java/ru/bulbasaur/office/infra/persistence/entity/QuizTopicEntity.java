package ru.bulbasaur.office.infra.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "quiz_topics")
@Getter
@Setter
@NoArgsConstructor
public class QuizTopicEntity {

    @Id
    private String code;

    private String name;

    @Column(name = "sort_order")
    private int sortOrder;
}
