package ru.bulbasaur.office.infra.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.bulbasaur.office.domain.model.GameId;
import ru.bulbasaur.office.infra.rest.dto.LeaderboardResponse;
import ru.bulbasaur.office.infra.rest.mapper.LeaderboardRestMapper;
import ru.bulbasaur.office.infra.security.AuthPrincipal;
import ru.bulbasaur.office.usecase.GetWotdLeaderboardUsecase;
import ru.bulbasaur.office.usecase.dto.GetWotdLeaderboardQuery;
import ru.bulbasaur.office.usecase.dto.LeaderboardView;
import ru.bulbasaur.office.usecase.exception.UnknownGameException;

@RestController
@RequestMapping("/api/leaderboard/wotd")
@RequiredArgsConstructor
public class WotdLeaderboardController {

    private static final int MAX_LIMIT = 100;

    private final GetWotdLeaderboardUsecase getWotdLeaderboardUsecase;
    private final LeaderboardRestMapper mapper;

    @GetMapping("/{game}")
    public LeaderboardResponse top(@PathVariable String game,
                                   @RequestParam(defaultValue = "20") int limit,
                                   @AuthenticationPrincipal AuthPrincipal player) {
        GameId gameId = resolve(game);
        int safeLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
        GetWotdLeaderboardQuery query = GetWotdLeaderboardQuery.builder()
                .game(gameId)
                .playerId(player.id())
                .login(player.login())
                .limit(safeLimit)
                .build();
        LeaderboardView view = getWotdLeaderboardUsecase.execute(query);
        return mapper.toResponse(view);
    }

    private GameId resolve(String code) {
        return GameId.fromCode(code).orElseThrow(() -> new UnknownGameException(code));
    }
}
