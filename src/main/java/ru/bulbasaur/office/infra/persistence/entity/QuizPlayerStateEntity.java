package ru.bulbasaur.office.infra.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "quiz_player_state")
@Getter
@Setter
@NoArgsConstructor
public class QuizPlayerStateEntity {

    @Id
    @Column(name = "player_id")
    private UUID playerId;

    private int level;

    private int energy;

    @Column(name = "energy_updated_at", nullable = false)
    private Instant energyUpdatedAt;

    @Column(name = "booster_reroll", nullable = false)
    private int boosterReroll;

    @Column(name = "booster_fifty", nullable = false)
    private int boosterFifty;

    @Column(name = "pending_chest", nullable = false)
    private boolean pendingChest;

    @Column(name = "chests_opened", nullable = false)
    private int chestsOpened;
}
