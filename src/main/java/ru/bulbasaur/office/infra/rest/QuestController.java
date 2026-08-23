package ru.bulbasaur.office.infra.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.bulbasaur.office.domain.model.QuestCode;
import ru.bulbasaur.office.infra.rest.dto.CompleteQuestRequest;
import ru.bulbasaur.office.infra.rest.dto.CompleteQuestResponse;
import ru.bulbasaur.office.infra.rest.dto.PresentationClaudeResponse;
import ru.bulbasaur.office.infra.rest.dto.PresentationDeckRequest;
import ru.bulbasaur.office.infra.rest.dto.PresentationDeckResponse;
import ru.bulbasaur.office.infra.rest.dto.QuestStatusResponse;
import ru.bulbasaur.office.infra.rest.dto.QuestsResponse;
import ru.bulbasaur.office.infra.security.AuthPrincipal;
import ru.bulbasaur.office.usecase.AcceptPresentationDeckUsecase;
import ru.bulbasaur.office.usecase.CompleteQuestUsecase;
import ru.bulbasaur.office.usecase.GetPresentationDeckUsecase;
import ru.bulbasaur.office.usecase.GetPresentationProgressUsecase;
import ru.bulbasaur.office.usecase.ListQuestsUsecase;
import ru.bulbasaur.office.usecase.PayPresentationClaudeUsecase;
import ru.bulbasaur.office.usecase.StartQuestUsecase;
import ru.bulbasaur.office.usecase.dto.PresentationClaudeView;
import ru.bulbasaur.office.usecase.dto.PresentationDeckView;
import ru.bulbasaur.office.usecase.dto.QuestCompleteView;
import ru.bulbasaur.office.usecase.dto.QuestStatusView;
import ru.bulbasaur.office.usecase.exception.UnknownQuestException;

@RestController
@RequestMapping("/api/quests")
@RequiredArgsConstructor
public class QuestController {

    private final ListQuestsUsecase listQuests;
    private final StartQuestUsecase startQuest;
    private final CompleteQuestUsecase completeQuest;
    private final GetPresentationProgressUsecase presentationProgress;
    private final PayPresentationClaudeUsecase payPresentationClaude;
    private final AcceptPresentationDeckUsecase acceptPresentationDeck;
    private final GetPresentationDeckUsecase presentationDeck;

    @GetMapping
    public QuestsResponse list(@AuthenticationPrincipal AuthPrincipal player) {
        return new QuestsResponse(listQuests.execute(player.id()).stream()
                .map(this::toStatus)
                .toList());
    }

    @PostMapping("/{code}/start")
    public QuestStatusResponse start(@PathVariable String code,
                                     @AuthenticationPrincipal AuthPrincipal player) {
        QuestStatusView view = startQuest.execute(player.id(), resolve(code));
        return toStatus(view);
    }

    @GetMapping("/presentation/claude")
    public PresentationClaudeResponse presentationClaude(@AuthenticationPrincipal AuthPrincipal player) {
        return toClaude(presentationProgress.execute(player.id()));
    }

    @PostMapping("/presentation/claude")
    public PresentationClaudeResponse payPresentationClaude(@AuthenticationPrincipal AuthPrincipal player) {
        return toClaude(payPresentationClaude.execute(player.id()));
    }

    @GetMapping("/presentation/deck")
    public PresentationDeckResponse getPresentationDeck(@AuthenticationPrincipal AuthPrincipal player) {
        return toDeck(presentationDeck.execute(player.id()));
    }

    @PostMapping("/presentation/deck")
    public PresentationDeckResponse postPresentationDeck(@Valid @RequestBody PresentationDeckRequest request,
                                                         @AuthenticationPrincipal AuthPrincipal player) {
        return toDeck(acceptPresentationDeck.execute(player.id(), request.slideIds()));
    }

    @PostMapping("/{code}/complete")
    public CompleteQuestResponse complete(@PathVariable String code,
                                          @Valid @RequestBody CompleteQuestRequest request,
                                          @AuthenticationPrincipal AuthPrincipal player) {
        QuestCode quest = resolve(code);
        QuestCompleteView view = completeQuest.execute(player.id(), quest, request.pin());
        return new CompleteQuestResponse(quest.code(), view.status(), view.bulbaCoinBalance(), view.rewarded());
    }

    private QuestStatusResponse toStatus(QuestStatusView view) {
        return new QuestStatusResponse(view.code(), view.status());
    }

    private PresentationClaudeResponse toClaude(PresentationClaudeView view) {
        return new PresentationClaudeResponse(view.claudePaid(), view.bulbaCoinBalance());
    }

    private PresentationDeckResponse toDeck(PresentationDeckView view) {
        return new PresentationDeckResponse(view.slideIds(), view.status());
    }

    private QuestCode resolve(String code) {
        return QuestCode.fromCode(code)
                .orElseThrow(() -> new UnknownQuestException(code));
    }
}
