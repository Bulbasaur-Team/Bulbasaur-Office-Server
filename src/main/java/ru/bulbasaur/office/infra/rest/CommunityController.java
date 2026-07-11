package ru.bulbasaur.office.infra.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.bulbasaur.office.domain.model.Role;
import ru.bulbasaur.office.infra.rest.dto.CommunityResponse;
import ru.bulbasaur.office.usecase.GetCommunityUsecase;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {

    private final GetCommunityUsecase getCommunity;

    @GetMapping
    public CommunityResponse list() {
        List<CommunityResponse.CommunityPlayerResponse> players = getCommunity.execute().stream()
                .map(p -> new CommunityResponse.CommunityPlayerResponse(
                        p.login(),
                        Optional.ofNullable(p.role()).map(Role::name).orElse(null),
                        p.ownedAchievements()))
                .toList();
        return new CommunityResponse(players, getCommunity.totalAchievements());
    }
}
