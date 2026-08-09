package ru.bulbasaur.office.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class QuizQuestion {

    private final UUID id;
    private final String topicCode;
    private final String text;
    private final List<String> options;
    private final int correctIndex;
}
