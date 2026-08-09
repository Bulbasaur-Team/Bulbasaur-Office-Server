package ru.bulbasaur.office.infra.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.bulbasaur.office.domain.model.QuizBooster;
import ru.bulbasaur.office.infra.rest.dto.QuizAnswerRequest;
import ru.bulbasaur.office.infra.rest.dto.QuizBoosterRequest;
import ru.bulbasaur.office.infra.rest.dto.QuizBuyBoosterRequest;
import ru.bulbasaur.office.infra.rest.dto.QuizStartAttemptRequest;
import ru.bulbasaur.office.infra.security.AuthPrincipal;
import ru.bulbasaur.office.usecase.quiz.AnswerQuizAttemptUsecase;
import ru.bulbasaur.office.usecase.quiz.BuyQuizBoosterUsecase;
import ru.bulbasaur.office.usecase.quiz.BuyQuizEnergyUsecase;
import ru.bulbasaur.office.usecase.quiz.GetQuizStateUsecase;
import ru.bulbasaur.office.usecase.quiz.GetQuizTopicsUsecase;
import ru.bulbasaur.office.usecase.quiz.OpenQuizChestUsecase;
import ru.bulbasaur.office.usecase.quiz.StartQuizAttemptUsecase;
import ru.bulbasaur.office.usecase.quiz.UseQuizBoosterUsecase;
import ru.bulbasaur.office.usecase.quiz.dto.QuizViews;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final GetQuizStateUsecase getStateUsecase;
    private final GetQuizTopicsUsecase getTopicsUsecase;
    private final BuyQuizEnergyUsecase buyEnergyUsecase;
    private final BuyQuizBoosterUsecase buyBoosterUsecase;
    private final StartQuizAttemptUsecase startAttemptUsecase;
    private final AnswerQuizAttemptUsecase answerAttemptUsecase;
    private final UseQuizBoosterUsecase useBoosterUsecase;
    private final OpenQuizChestUsecase openChestUsecase;

    @GetMapping("/state")
    public QuizViews.StateView state(@AuthenticationPrincipal AuthPrincipal player) {
        return getStateUsecase.execute(player.id());
    }

    @GetMapping("/topics")
    public Map<String, List<QuizViews.TopicView>> topics(@AuthenticationPrincipal AuthPrincipal player) {
        return Map.of("topics", getTopicsUsecase.execute(player.id()));
    }

    @PostMapping("/energy/buy")
    public QuizViews.StateView buyEnergy(@AuthenticationPrincipal AuthPrincipal player) {
        return buyEnergyUsecase.execute(player.id());
    }

    @PostMapping("/boosters/buy")
    public QuizViews.StateView buyBooster(@Valid @RequestBody QuizBuyBoosterRequest request,
                                         @AuthenticationPrincipal AuthPrincipal player) {
        return buyBoosterUsecase.execute(player.id(), requireBooster(request.type()));
    }

    @PostMapping("/attempt/start")
    public QuizViews.AttemptView start(@Valid @RequestBody QuizStartAttemptRequest request,
                                       @AuthenticationPrincipal AuthPrincipal player) {
        return startAttemptUsecase.execute(player.id(), request.topicCode());
    }

    @PostMapping("/attempt/answer")
    public QuizViews.AttemptView answer(@Valid @RequestBody QuizAnswerRequest request,
                                        @AuthenticationPrincipal AuthPrincipal player) {
        return answerAttemptUsecase.execute(player.id(), player.login(), request.attemptId(), request.optionIndex());
    }

    @PostMapping("/attempt/booster")
    public QuizViews.AttemptView booster(@Valid @RequestBody QuizBoosterRequest request,
                                         @AuthenticationPrincipal AuthPrincipal player) {
        return useBoosterUsecase.execute(player.id(), request.attemptId(), requireBooster(request.type()));
    }

    @PostMapping("/chest/open")
    public QuizViews.ChestRewardView openChest(@AuthenticationPrincipal AuthPrincipal player) {
        return openChestUsecase.execute(player.id(), player.login());
    }

    private static QuizBooster requireBooster(String code) {
        return QuizBooster.fromCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Неизвестный бустер: " + code));
    }
}
