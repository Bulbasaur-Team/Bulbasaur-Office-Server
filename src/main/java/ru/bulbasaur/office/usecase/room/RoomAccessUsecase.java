package ru.bulbasaur.office.usecase.room;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.infra.persistence.entity.PokerRoomEntity;
import ru.bulbasaur.office.infra.persistence.entity.PokerRoomMemberEntity;
import ru.bulbasaur.office.infra.persistence.entity.RetroRoomEntity;
import ru.bulbasaur.office.infra.persistence.entity.RetroRoomMemberEntity;
import ru.bulbasaur.office.infra.persistence.repository.PokerRoomJpaRepository;
import ru.bulbasaur.office.infra.persistence.repository.PokerRoomMemberJpaRepository;
import ru.bulbasaur.office.infra.persistence.repository.RetroRoomJpaRepository;
import ru.bulbasaur.office.infra.persistence.repository.RetroRoomMemberJpaRepository;
import ru.bulbasaur.office.usecase.exception.RoomLockedException;
import ru.bulbasaur.office.usecase.exception.RoomNotFoundException;
import ru.bulbasaur.office.usecase.port.out.PasswordHasherPort;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * Пароль комнаты poker/retro. Хэш пустой — комната открыта всем.
 * Иначе содержимое видит только тот, кто уже в списке допущенных.
 * Верный пароль добавляет игрока в список навсегда.
 */
@Service
@RequiredArgsConstructor
public class RoomAccessUsecase {

    public static final int MAX_PASSWORD_LENGTH = 72;

    private final PasswordHasherPort hasher;
    private final PokerRoomJpaRepository pokerRooms;
    private final PokerRoomMemberJpaRepository pokerMembers;
    private final RetroRoomJpaRepository retroRooms;
    private final RetroRoomMemberJpaRepository retroMembers;

    /** null — пароль не задан (комнату можно не закрывать). */
    public String hashOptional(String raw) {
        if (raw == null) {
            return null;
        }
        String password = raw.strip();
        if (password.isEmpty()) {
            return null;
        }
        if (tooLong(password)) {
            throw new IllegalArgumentException(RoomLockedException.TOO_LONG);
        }
        return hasher.hash(password);
    }

    @Transactional
    public void grantPoker(UUID roomId, UUID playerId) {
        pokerMembers.grant(roomId, playerId);
    }

    @Transactional
    public void grantRetro(UUID roomId, UUID playerId) {
        retroMembers.grant(roomId, playerId);
    }

    @Transactional(readOnly = true)
    public Set<UUID> pokerMemberRoomIds(UUID playerId) {
        if (playerId == null) {
            return Set.of();
        }
        return pokerMembers.findRoomIdsByPlayerId(playerId);
    }

    @Transactional(readOnly = true)
    public Set<UUID> retroMemberRoomIds(UUID playerId) {
        if (playerId == null) {
            return Set.of();
        }
        return retroMembers.findRoomIdsByPlayerId(playerId);
    }

    public static boolean needsPassword(boolean locked, UUID roomId, Set<UUID> allowed) {
        return locked && (allowed == null || !allowed.contains(roomId));
    }

    /** Пустой пароль у уже допущенного или у открытой комнаты — успех. */
    @Transactional
    public void admitPoker(UUID roomId, UUID playerId, String rawPassword) {
        PokerRoomEntity room = pokerRooms.findById(roomId).orElseThrow(RoomNotFoundException::new);
        admit(
                room.getPasswordHash(),
                rawPassword,
                () -> pokerMembers.existsById(new PokerRoomMemberEntity.Pk(roomId, playerId)),
                () -> pokerMembers.grant(roomId, playerId));
    }

    @Transactional
    public void admitRetro(UUID roomId, UUID playerId, String rawPassword) {
        RetroRoomEntity room = retroRooms.findById(roomId).orElseThrow(RoomNotFoundException::new);
        admit(
                room.getPasswordHash(),
                rawPassword,
                () -> retroMembers.existsById(new RetroRoomMemberEntity.Pk(roomId, playerId)),
                () -> retroMembers.grant(roomId, playerId));
    }

    private void admit(String passwordHash, String rawPassword, BooleanSupplier alreadyMember, Runnable grant) {
        if (passwordHash == null || passwordHash.isBlank()) {
            return;
        }
        if (alreadyMember.getAsBoolean()) {
            return;
        }
        String password = rawPassword == null ? "" : rawPassword.strip();
        if (password.isEmpty()) {
            throw new RoomLockedException(RoomLockedException.REQUIRED);
        }
        if (tooLong(password)) {
            throw new RoomLockedException(RoomLockedException.TOO_LONG);
        }
        if (!hasher.matches(password, passwordHash)) {
            throw new RoomLockedException(RoomLockedException.WRONG);
        }
        grant.run();
    }

    private static boolean tooLong(String password) {
        return password.length() > MAX_PASSWORD_LENGTH
                || password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_LENGTH;
    }
}
