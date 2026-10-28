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
@Table(name = "retro_rooms")
@Getter
@Setter
@NoArgsConstructor
public class RetroRoomEntity {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_CLOSED = "CLOSED";

    @Id
    private UUID id;

    private String name;

    private UUID adminPlayerId;

    /** Bcrypt-хэш. null — комната без пароля. */
    private String passwordHash;

    private String status;

    public boolean isLocked() {
        return passwordHash != null && !passwordHash.isBlank();
    }

    private Instant createdAt;

    private Instant closesAt;

    private Instant closedAt;
}
