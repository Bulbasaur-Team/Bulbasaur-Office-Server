package ru.bulbasaur.office.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class QuizTopic {

    private final String code;
    private final String name;
    private final int sortOrder;
}
