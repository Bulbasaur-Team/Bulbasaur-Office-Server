package ru.bulbasaur.office.infra.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.bulbasaur.office.infra.rest.dto.RoomPasswordRequest;
import ru.bulbasaur.office.infra.security.AuthPrincipal;
import ru.bulbasaur.office.infra.ws.dto.PokerStateOut;
import ru.bulbasaur.office.usecase.poker.BuildPokerHistoryStateUsecase;
import ru.bulbasaur.office.usecase.room.RoomAccessUsecase;

import java.util.UUID;

@RestController
@RequestMapping("/api/poker")
@RequiredArgsConstructor
public class PokerController {

    private final BuildPokerHistoryStateUsecase buildHistory;
    private final RoomAccessUsecase roomAccess;

    @GetMapping("/rooms/{roomId}")
    public PokerStateOut getRoom(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal AuthPrincipal player
    ) {
        roomAccess.admitPoker(roomId, player.id(), null);
        return buildHistory.execute(roomId, player.id());
    }

    @PostMapping("/rooms/{roomId}/access")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void grantAccess(
            @PathVariable UUID roomId,
            @RequestBody RoomPasswordRequest body,
            @AuthenticationPrincipal AuthPrincipal player
    ) {
        roomAccess.admitPoker(roomId, player.id(), body.password());
    }
}
