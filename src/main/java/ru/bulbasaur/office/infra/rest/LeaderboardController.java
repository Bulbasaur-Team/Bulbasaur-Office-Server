package ru.bulbasaur.office.infra.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.bulbasaur.office.domain.model.GameId;
import ru.bulbasaur.office.infra.rest.dto.LeaderboardResponse;
import ru.bulbasaur.office.infra.rest.dto.SubmitScoreRequest;
import ru.bulbasaur.office.infra.rest.mapper.LeaderboardRestMapper;
import ru.bulbasaur.office.infra.security.AuthPrincipal;
import ru.bulbasaur.office.usecase.GetLeaderboardUsecase;
import ru.bulbasaur.office.usecase.SubmitScoreUsecase;
import ru.bulbasaur.office.usecase.dto.LeaderboardView;
import ru.bulbasaur.office.usecase.exception.UnknownGameException;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final SubmitScoreUsecase submitScoreUsecase;
    private final GetLeaderboardUsecase getLeaderboardUsecase;
    private final LeaderboardRestMapper mapper;

    @PostMapping("/{game}")
    public LeaderboardResponse submit(@PathVariable String game,
                                      @Valid @RequestBody SubmitScoreRequest request,
                                      @AuthenticationPrincipal AuthPrincipal player) {
        GameId gameId = resolve(game);
        LeaderboardView view = submitScoreUsecase.execute(
                player.id(), player.login(), gameId, request.value(), DEFAULT_LIMIT);
        return mapper.toResponse(view);
    }

    @GetMapping("/{game}")
    public LeaderboardResponse top(@PathVariable String game,
                                   @RequestParam(defaultValue = "20") int limit,
                                   @AuthenticationPrincipal AuthPrincipal player) {
        GameId gameId = resolve(game);
        int safeLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
        LeaderboardView view = getLeaderboardUsecase.execute(gameId, player.id(), player.login(), safeLimit);
        return mapper.toResponse(view);
    }

    private GameId resolve(String code) {
        return GameId.fromCode(code).orElseThrow(() -> new UnknownGameException(code));
    }
}
