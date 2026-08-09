package ru.bulbasaur.office.usecase.quiz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.domain.model.QuizPlayerState;
import ru.bulbasaur.office.domain.model.QuizTopic;
import ru.bulbasaur.office.usecase.port.out.QuizRepositoryPort;
import ru.bulbasaur.office.usecase.quiz.dto.QuizViews;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetQuizTopicsUsecase {

    private final QuizRepositoryPort quiz;

    @Transactional
    public List<QuizViews.TopicView> execute(UUID playerId) {
        QuizPlayerState state = quiz.getOrCreateState(playerId);
        int levelToPlay = state.getLevel() + 1;
        List<QuizTopic> topics = new ArrayList<>(quiz.topics());

        // Порядок зависит только от номера уровня и стабильного порядка тем из БД.
        // Повторное открытие уровня всегда возвращает ту же тройку.
        long seed = 0xB01BA5EEDL ^ (long) levelToPlay * 0x9E3779B97F4A7C15L;
        Collections.shuffle(topics, new Random(seed));

        return topics.stream()
                .limit(3)
                .map(t -> new QuizViews.TopicView(t.getCode(), t.getName()))
                .toList();
    }
}
