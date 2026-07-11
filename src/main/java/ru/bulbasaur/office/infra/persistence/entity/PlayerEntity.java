package ru.bulbasaur.office.infra.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.bulbasaur.office.domain.model.Role;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "players")
@Getter
@Setter
@NoArgsConstructor
public class PlayerEntity {

    @Id
    private UUID id;

    private String login;

    private String passwordHash;

    private Instant createdAt;

    /** Выбранная роль (скин Бульбазавра); null — игрок ещё не выбирал. */
    @Enumerated(EnumType.STRING)
    private Role role;
}
