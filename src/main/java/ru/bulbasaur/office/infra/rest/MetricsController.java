package ru.bulbasaur.office.infra.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.bulbasaur.office.infra.rest.dto.OfficeMetricsResponse;
import ru.bulbasaur.office.infra.rest.dto.OfficeMetricsResponse.OfficeMetricsPointResponse;
import ru.bulbasaur.office.infra.security.AuthPrincipal;
import ru.bulbasaur.office.usecase.GetOfficeMetricsUsecase;
import ru.bulbasaur.office.usecase.RecordOfficeMetricsTickUsecase;
import ru.bulbasaur.office.usecase.dto.OfficeMetricsPoint;

import java.util.List;

@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class MetricsController {

    private final GetOfficeMetricsUsecase getMetrics;

    @GetMapping
    public OfficeMetricsResponse metrics(@AuthenticationPrincipal AuthPrincipal player) {
        List<OfficeMetricsPointResponse> points = getMetrics.execute(player.id()).stream()
                .map(MetricsController::toResponse)
                .toList();
        return new OfficeMetricsResponse(RecordOfficeMetricsTickUsecase.BUCKET_SECONDS / 60, points);
    }

    private static OfficeMetricsPointResponse toResponse(OfficeMetricsPoint p) {
        return new OfficeMetricsPointResponse(
                p.bucketStart().toString(),
                p.onlineCount(),
                p.tennisKicks(),
                p.volleyballKicks(),
                p.coffeeCups());
    }
}
