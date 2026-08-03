package ru.bulbasaur.office.usecase.dto;

import java.time.Instant;
import java.util.UUID;

public record BulbaCoinTransactionView(
        UUID id,
        long amount,
        String kind,
        String title,
        Instant createdAt
) {
}
