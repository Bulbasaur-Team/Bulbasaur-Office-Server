package ru.bulbasaur.office.usecase.retro;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.infra.persistence.entity.RetroRoomEntity;
import ru.bulbasaur.office.infra.persistence.repository.RetroRoomJpaRepository;
import ru.bulbasaur.office.infra.ws.RetroRoom;
import ru.bulbasaur.office.usecase.room.RoomAccessUsecase;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateRetroRoomUsecase {

    private final RetroRoomJpaRepository rooms;
    private final RoomAccessUsecase roomAccess;

    @Transactional
    public RetroRoomEntity execute(String name, UUID adminPlayerId, String passwordHash) {
        if (rooms.countByStatus(RetroRoomEntity.STATUS_ACTIVE) >= RetroConstants.MAX_ACTIVE_ROOMS) {
            return null;
        }
        Instant now = Instant.now();
        RetroRoomEntity entity = new RetroRoomEntity();
        entity.setId(UUID.randomUUID());
        entity.setName(name);
        entity.setAdminPlayerId(adminPlayerId);
        entity.setPasswordHash(passwordHash);
        entity.setStatus(RetroRoomEntity.STATUS_ACTIVE);
        entity.setCreatedAt(now);
        entity.setClosesAt(now.plusMillis(RetroRoom.TTL_MS));
        RetroRoomEntity saved = rooms.saveAndFlush(entity);
        if (passwordHash != null) {
            roomAccess.grantRetro(saved.getId(), adminPlayerId);
        }
        return saved;
    }
}
