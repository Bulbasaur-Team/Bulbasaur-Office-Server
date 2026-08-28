package ru.bulbasaur.office.infra.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "player_presentation_deck")
@Getter
@Setter
@NoArgsConstructor
public class PlayerPresentationDeckEntity {

    @Id
    private UUID playerId;

    /** JSON-массив id слайдов в порядке Дня X. */
    private String slideIds;

    private Instant updatedAt;
}
