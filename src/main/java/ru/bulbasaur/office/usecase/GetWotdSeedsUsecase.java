package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bulbasaur.office.infra.config.AppProperties;
import ru.bulbasaur.office.usecase.dto.WotdSeedsView;
import ru.bulbasaur.office.usecase.dto.WotdSeedsView.GameSeeds;
import ru.bulbasaur.office.usecase.port.out.WotdSeedPort;
import ru.bulbasaur.office.usecase.port.out.DayPort;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class GetWotdSeedsUsecase {

    private static final String GUESS = "bulbaguess";
    private static final String WORDLE = "bulbawordle";

    private final WotdSeedPort seeds;
    private final DayPort day;
    private final AppProperties properties;

    public WotdSeedsView execute() {
        LocalDate today = day.today();
        LocalDate yesterday = today.minusDays(1);
        return new WotdSeedsView(
                gameSeeds(GUESS, today, yesterday),
                gameSeeds(WORDLE, today, yesterday)
        );
    }

    private GameSeeds gameSeeds(String namespace, LocalDate today, LocalDate yesterday) {
        String prev = yesterday.isBefore(properties.wotd().launchDate()) ? null : seeds.seed(namespace, yesterday);
        return new GameSeeds(seeds.seed(namespace, today), prev);
    }
}
