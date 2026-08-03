package ru.bulbasaur.office.usecase.dto;

import java.util.List;

public record BulbaCoinHistoryView(long balance, List<BulbaCoinTransactionView> transactions) {
}
