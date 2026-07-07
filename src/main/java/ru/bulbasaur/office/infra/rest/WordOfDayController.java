package ru.bulbasaur.office.infra.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.bulbasaur.office.infra.rest.dto.WotdSeedsResponse;
import ru.bulbasaur.office.infra.rest.dto.WotdSeedsResponse.GameSeedsResponse;
import ru.bulbasaur.office.usecase.GetWotdSeedsUsecase;
import ru.bulbasaur.office.usecase.dto.WotdSeedsView;

@RestController
@RequestMapping("/api/wotd")
@RequiredArgsConstructor
public class WordOfDayController {

    private final GetWotdSeedsUsecase getWotdSeeds;

    @GetMapping
    public WotdSeedsResponse get() {
        WotdSeedsView view = getWotdSeeds.execute();
        return new WotdSeedsResponse(
                new GameSeedsResponse(view.guess().today(), view.guess().prev()),
                new GameSeedsResponse(view.wordle().today(), view.wordle().prev()));
    }
}
