package ru.bulbasaur.office.infra.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.bulbasaur.office.infra.rest.dto.AchievementResponse;
import ru.bulbasaur.office.infra.rest.dto.AchievementsResponse;
import ru.bulbasaur.office.infra.security.AuthPrincipal;
import ru.bulbasaur.office.usecase.AchievementService;
import ru.bulbasaur.office.usecase.dto.AchievementView;

import java.util.List;

@RestController
@RequestMapping("/api/achievements")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementService achievements;

    @GetMapping
    public AchievementsResponse list(@AuthenticationPrincipal AuthPrincipal player) {
        List<AchievementView> views = achievements.list(player.id());
        List<AchievementResponse> items = views.stream()
                .map(v -> new AchievementResponse(v.code(), v.title(), v.description(), v.image(), v.owned()))
                .toList();
        int owned = (int) items.stream().filter(AchievementResponse::owned).count();
        return new AchievementsResponse(items, owned, items.size());
    }
}
