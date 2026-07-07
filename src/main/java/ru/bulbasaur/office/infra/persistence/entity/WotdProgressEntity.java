package ru.bulbasaur.office.infra.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.bulbasaur.office.domain.model.GameId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "wotd_progress")
@Getter
@Setter
@NoArgsConstructor
public class WotdProgressEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private PlayerEntity player;

    @Enumerated(EnumType.STRING)
    private GameId game;

    private LocalDate day;

    private boolean solved;

    private int attempts;

    private String guesses;

    private Instant updatedAt;
}
