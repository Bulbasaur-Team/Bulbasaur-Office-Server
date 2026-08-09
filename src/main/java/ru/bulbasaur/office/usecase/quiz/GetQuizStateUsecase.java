package ru.bulbasaur.office.usecase.quiz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.usecase.quiz.dto.QuizViews;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetQuizStateUsecase {

    private final QuizStateHelper helper;

    @Transactional
    public QuizViews.StateView execute(UUID playerId) {
        return helper.toView(helper.loadWithRegen(playerId, Instant.now()), playerId);
    }
}
