package ru.bulbasaur.office.infra.scheduling;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.bulbasaur.office.usecase.RecordOfficeMetricsTickUsecase;

/** Каждые 5 минут пишет снимок метрик офиса в БД. */
@Component
@RequiredArgsConstructor
public class OfficeMetricsSnapshotJob {

    private final RecordOfficeMetricsTickUsecase recordTick;

    @Scheduled(cron = "0 */5 * * * *")
    public void snapshot() {
        recordTick.execute();
    }
}
