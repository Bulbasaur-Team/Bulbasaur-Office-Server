package ru.bulbasaur.office.infra.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.bulbasaur.office.infra.security.AuthPrincipal;
import ru.bulbasaur.office.infra.ws.dto.PokerStateOut;
import ru.bulbasaur.office.usecase.poker.BuildPokerHistoryStateUsecase;

import java.util.UUID;

@RestController
@RequestMapping("/api/poker")
@RequiredArgsConstructor
public class PokerController {

    private final BuildPokerHistoryStateUsecase buildHistory;

    @GetMapping("/rooms/{roomId}")
    public PokerStateOut getRoom(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal AuthPrincipal player
    ) {
        return buildHistory.execute(roomId, player.id());
    }
}
