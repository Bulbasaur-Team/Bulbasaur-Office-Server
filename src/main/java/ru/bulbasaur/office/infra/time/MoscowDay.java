package ru.bulbasaur.office.infra.time;

import org.springframework.stereotype.Component;
import ru.bulbasaur.office.usecase.port.out.DayPort;

import java.time.LocalDate;
import java.time.ZoneId;

/** Текущая московская дата (GMT+3). Смена слова дня — в 00:00 этой зоны. */
@Component
public class MoscowDay implements DayPort {

    private static final ZoneId MSK = ZoneId.of("Europe/Moscow");

    @Override
    public LocalDate today() {
        return LocalDate.now(MSK);
    }
}
