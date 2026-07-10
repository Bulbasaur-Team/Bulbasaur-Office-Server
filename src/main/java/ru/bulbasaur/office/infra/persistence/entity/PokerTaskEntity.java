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
@Table(name = "poker_tasks")
@Getter
@Setter
@NoArgsConstructor
public class PokerTaskEntity {

    @Id
    private UUID id;

    private String roomName;

    private String title;

    private Double average;

    private Integer recommended;

    private Instant createdAt;
}
