package ru.bulbasaur.office.infra.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.bulbasaur.office.infra.rest.dto.GuessHintResponse;
import ru.bulbasaur.office.infra.security.AuthPrincipal;
import ru.bulbasaur.office.usecase.BuyGuessHintUsecase;

@RestController
@RequestMapping("/api/guess")
@RequiredArgsConstructor
public class GuessController {

    private final BuyGuessHintUsecase buyGuessHint;

    @PostMapping("/hint")
    public GuessHintResponse buyHint(@AuthenticationPrincipal AuthPrincipal player) {
        return new GuessHintResponse(buyGuessHint.execute(player.id()));
    }
}
