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

/** Сезон Bulba Jump. Пока {@code awardedAt} пуст, сезон ещё не закрыт. */
@Entity
@Table(name = "jump_season")
@Getter
@Setter
@NoArgsConstructor
public class JumpSeasonEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private int seasonNumber;

    @Column(nullable = false)
    private Instant startsAt;

    @Column(nullable = false)
    private Instant endsAt;

    private Instant awardedAt;
}
