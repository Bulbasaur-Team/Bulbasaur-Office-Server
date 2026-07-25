package ru.bulbasaur.office.usecase.retro;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.infra.persistence.entity.RetroRoomEntity;
import ru.bulbasaur.office.infra.persistence.repository.RetroRoomJpaRepository;
import ru.bulbasaur.office.infra.ws.RetroRoom;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateRetroRoomUsecase {

    private final RetroRoomJpaRepository rooms;

    @Transactional
    public RetroRoomEntity execute(String name, UUID adminPlayerId) {
        if (rooms.countByStatus(RetroRoomEntity.STATUS_ACTIVE) >= RetroConstants.MAX_ACTIVE_ROOMS) {
            return null;
        }
        Instant now = Instant.now();
        RetroRoomEntity entity = new RetroRoomEntity();
        entity.setId(UUID.randomUUID());
        entity.setName(name);
        entity.setAdminPlayerId(adminPlayerId);
        entity.setStatus(RetroRoomEntity.STATUS_ACTIVE);
        entity.setCreatedAt(now);
        entity.setClosesAt(now.plusMillis(RetroRoom.TTL_MS));
        return rooms.save(entity);
    }
}
