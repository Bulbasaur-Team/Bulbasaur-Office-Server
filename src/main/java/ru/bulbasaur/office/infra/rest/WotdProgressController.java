package ru.bulbasaur.office.infra.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.bulbasaur.office.domain.model.GameId;
import ru.bulbasaur.office.infra.rest.dto.WotdProgressRequest;
import ru.bulbasaur.office.infra.rest.dto.WotdProgressResponse;
import ru.bulbasaur.office.infra.security.AuthPrincipal;
import ru.bulbasaur.office.usecase.GetWotdProgressUsecase;
import ru.bulbasaur.office.usecase.SaveWotdProgressUsecase;
import ru.bulbasaur.office.usecase.dto.WotdProgressView;
import ru.bulbasaur.office.usecase.dto.SaveWotdProgressCommand;
import ru.bulbasaur.office.usecase.exception.UnknownGameException;

@RestController
@RequestMapping("/api/wotd")
@RequiredArgsConstructor
public class WotdProgressController {

    private final GetWotdProgressUsecase getWotdProgress;
    private final SaveWotdProgressUsecase saveWotdProgress;

    @GetMapping("/{game}/progress")
    public WotdProgressResponse get(@PathVariable String game,
                                     @AuthenticationPrincipal AuthPrincipal player) {
        WotdProgressView view = getWotdProgress.execute(resolveGameId(game), player.id());
        return toResponse(view);
    }

    @PutMapping("/{game}/progress")
    public WotdProgressResponse save(@PathVariable String game,
                                      @Valid @RequestBody WotdProgressRequest request,
                                      @AuthenticationPrincipal AuthPrincipal player) {
        SaveWotdProgressCommand command = SaveWotdProgressCommand.builder()
                .playerId(player.id())
                .game(resolveGameId(game))
                .solved(request.solved())
                .attempts(request.attempts())
                .guesses(request.guesses())
                .build();
        WotdProgressView view = saveWotdProgress.execute(command);
        return toResponse(view);
    }

    private WotdProgressResponse toResponse(WotdProgressView view) {
        return new WotdProgressResponse(view.solved(), view.attempts(), view.guesses());
    }

    private GameId resolveGameId(String code) {
        return GameId.fromCode(code)
                .orElseThrow(() -> new UnknownGameException(code));
    }
}
