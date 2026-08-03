package ru.bulbasaur.office.infra.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "player_equipped")
@IdClass(PlayerEquippedEntity.Pk.class)
@Getter
@Setter
@NoArgsConstructor
public class PlayerEquippedEntity {

    @Id
    private UUID playerId;

    @Id
    private String category;

    private String itemCode;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Pk implements Serializable {
        private UUID playerId;
        private String category;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pk pk)) return false;
            return Objects.equals(playerId, pk.playerId) && Objects.equals(category, pk.category);
        }

        @Override
        public int hashCode() {
            return Objects.hash(playerId, category);
        }
    }
}
