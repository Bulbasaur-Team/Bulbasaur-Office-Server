package ru.bulbasaur.office.infra.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.bulbasaur.office.domain.model.Achievement;
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
        return toResponse(achievements.list(player.id()), achievements.countOwnedPublic(player.id()));
    }

    /** Ачивки другого игрока — для экрана сообщества. */
    @GetMapping("/{login}")
    public AchievementsResponse listOf(@PathVariable String login) {
        List<AchievementView> views = achievements.listByLogin(login);
        int owned = (int) views.stream().filter(AchievementView::owned).count();
        return toResponse(views, owned);
    }

    private AchievementsResponse toResponse(List<AchievementView> views, int owned) {
        List<AchievementResponse> items = views.stream()
                .map(v -> new AchievementResponse(
                        v.code(), v.title(), v.description(), v.image(), v.owned(), v.percent()))
                .toList();
        return new AchievementsResponse(items, owned, Achievement.publicCount());
    }
}
