package ru.bulbasaur.office.infra.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Лучший результат игрока внутри одного сезона Bulba Jump. */
@Entity
@Table(name = "jump_season_score")
@Getter
@Setter
@NoArgsConstructor
public class JumpSeasonScoreEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private JumpSeasonEntity season;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerEntity player;

    private long value;

    private Instant updatedAt;
}
