package ru.bulbasaur.office.infra.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.bulbasaur.office.infra.rest.dto.JumpSeasonResponse;
import ru.bulbasaur.office.infra.rest.dto.LeaderboardEntryResponse;
import ru.bulbasaur.office.infra.security.AuthPrincipal;
import ru.bulbasaur.office.usecase.JumpSeasonUsecase;
import ru.bulbasaur.office.usecase.dto.JumpSeasonView;
import ru.bulbasaur.office.usecase.dto.LeaderboardRowView;

import java.time.Instant;

@RestController
@RequestMapping("/api/jump-season")
@RequiredArgsConstructor
public class JumpSeasonController {

    private static final int LIMIT = 20;

    private final JumpSeasonUsecase seasons;

    @GetMapping
    public JumpSeasonResponse current(@AuthenticationPrincipal AuthPrincipal player) {
        JumpSeasonView view = seasons.current(player.id(), player.login(), LIMIT, Instant.now());
        return new JumpSeasonResponse(
                view.number(),
                view.startsAt(),
                view.endsAt(),
                view.serverNow(),
                view.prizes(),
                new JumpSeasonResponse.PreviousWinner(
                        view.previousWinner().season(),
                        view.previousWinner().login(),
                        view.previousWinner().value()),
                view.top().stream().map(this::toEntry).toList(),
                view.me() == null ? null : toEntry(view.me()));
    }

    private LeaderboardEntryResponse toEntry(LeaderboardRowView row) {
        return new LeaderboardEntryResponse(row.rank(), row.login(), row.value(), row.currentPlayer());
    }
}
