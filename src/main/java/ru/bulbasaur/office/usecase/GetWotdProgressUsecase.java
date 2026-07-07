package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bulbasaur.office.domain.model.GameId;
import ru.bulbasaur.office.usecase.dto.WotdProgressView;
import ru.bulbasaur.office.usecase.port.out.WotdProgressRepositoryPort;
import ru.bulbasaur.office.usecase.port.out.DayPort;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetWotdProgressUsecase {

    private final WotdProgressRepositoryPort progress;
    private final DayPort day;

    public WotdProgressView execute(GameId game, UUID playerId) {
        return progress.findPlayerProgress(playerId, game, day.today()).orElseGet(WotdProgressView::empty);
    }
}
