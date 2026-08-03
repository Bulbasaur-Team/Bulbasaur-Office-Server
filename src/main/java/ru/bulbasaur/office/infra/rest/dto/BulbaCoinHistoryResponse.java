package ru.bulbasaur.office.infra.rest.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BulbaCoinHistoryResponse(long balance, List<Transaction> transactions) {
    public record Transaction(UUID id, long amount, String kind, String title, Instant createdAt) {
    }
}
