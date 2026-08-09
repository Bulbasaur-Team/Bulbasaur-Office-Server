package ru.bulbasaur.office.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class QuizPlayerState {

    private UUID playerId;
    private int level;
    private int energy;
    private Instant energyUpdatedAt;
    private int boosterReroll;
    private int boosterFifty;
    private boolean pendingChest;
    private int chestsOpened;
}
