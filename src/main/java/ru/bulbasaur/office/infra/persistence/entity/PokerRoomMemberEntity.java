package ru.bulbasaur.office.infra.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/** Игрок, которому уже можно открывать запароленную покер-комнату без повторного пароля. */
@Entity
@Table(name = "poker_room_members")
@IdClass(PokerRoomMemberEntity.Pk.class)
@Getter
@Setter
@NoArgsConstructor
public class PokerRoomMemberEntity {

    @Id
    private UUID roomId;

    @Id
    private UUID playerId;

    private Instant grantedAt;

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Pk implements Serializable {
        private UUID roomId;
        private UUID playerId;
    }
}
