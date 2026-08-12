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
@Table(name = "poker_rooms")
@Getter
@Setter
@NoArgsConstructor
public class PokerRoomEntity {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_CLOSED = "CLOSED";

    @Id
    private UUID id;

    private String name;

    private UUID adminPlayerId;

    private String status;

    private Instant createdAt;

    private Instant closesAt;

    private Instant closedAt;
}
