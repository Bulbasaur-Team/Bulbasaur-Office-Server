package ru.bulbasaur.office.usecase.retro;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bulbasaur.office.domain.model.Role;
import ru.bulbasaur.office.infra.persistence.entity.PlayerEntity;
import ru.bulbasaur.office.infra.persistence.entity.RetroRoomEntity;
import ru.bulbasaur.office.infra.persistence.repository.PlayerJpaRepository;
import ru.bulbasaur.office.infra.persistence.repository.RetroRoomJpaRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Чтение комнат ретро и имён игроков. */
@Service
@RequiredArgsConstructor
public class GetRetroRoomsUsecase {

    private final RetroRoomJpaRepository rooms;
    private final PlayerJpaRepository players;

    public Optional<RetroRoomEntity> findRoom(UUID roomId) {
        return rooms.findById(roomId);
    }

    public List<RetroRoomEntity> findActiveRooms() {
        return rooms.findByStatusOrderByCreatedAtDesc(RetroRoomEntity.STATUS_ACTIVE);
    }

    public List<RetroRoomEntity> findClosedRooms() {
        return rooms.findByStatusOrderByCreatedAtDesc(RetroRoomEntity.STATUS_CLOSED);
    }

    public Map<UUID, String> loginMap(Set<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return players.findAllById(ids).stream()
                .collect(Collectors.toMap(PlayerEntity::getId, PlayerEntity::getLogin));
    }

    public Map<UUID, String> roleMap(Set<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> out = new HashMap<>();
        for (PlayerEntity p : players.findAllById(ids)) {
            Role role = p.getRole();
            out.put(p.getId(), role == null ? null : role.name());
        }
        return out;
    }
}
