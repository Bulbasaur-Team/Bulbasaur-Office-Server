package ru.bulbasaur.office.usecase.room;

import org.junit.jupiter.api.Test;
import ru.bulbasaur.office.infra.persistence.entity.PokerRoomEntity;
import ru.bulbasaur.office.infra.persistence.entity.PokerRoomMemberEntity;
import ru.bulbasaur.office.infra.persistence.entity.RetroRoomEntity;
import ru.bulbasaur.office.infra.persistence.entity.RetroRoomMemberEntity;
import ru.bulbasaur.office.infra.persistence.repository.PokerRoomJpaRepository;
import ru.bulbasaur.office.infra.persistence.repository.PokerRoomMemberJpaRepository;
import ru.bulbasaur.office.infra.persistence.repository.RetroRoomJpaRepository;
import ru.bulbasaur.office.infra.persistence.repository.RetroRoomMemberJpaRepository;
import ru.bulbasaur.office.infra.security.BCryptPasswordHasher;
import ru.bulbasaur.office.usecase.exception.RoomLockedException;
import ru.bulbasaur.office.usecase.port.out.PasswordHasherPort;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoomAccessUsecaseTest {

    private static final Pattern HASH = Pattern.compile("\\$2a\\$\\d{2}\\$[A-Za-z0-9./]{53}");

    private final PasswordHasherPort hasher = new BCryptPasswordHasher();
    private final PokerRoomJpaRepository pokerRooms = mock(PokerRoomJpaRepository.class);
    private final PokerRoomMemberJpaRepository pokerMembers = mock(PokerRoomMemberJpaRepository.class);
    private final RetroRoomJpaRepository retroRooms = mock(RetroRoomJpaRepository.class);
    private final RetroRoomMemberJpaRepository retroMembers = mock(RetroRoomMemberJpaRepository.class);
    private final RoomAccessUsecase access = new RoomAccessUsecase(
            hasher, pokerRooms, pokerMembers, retroRooms, retroMembers);

    @Test
    void blankPasswordMeansOpenRoom() {
        assertThat(access.hashOptional(null)).isNull();
        assertThat(access.hashOptional("   ")).isNull();
        assertThat(access.hashOptional("secret")).isNotBlank();
    }

    @Test
    void passwordLongerThanBcryptLimitIsRejected() {
        assertThatThrownBy(() -> access.hashOptional("a".repeat(73)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(RoomLockedException.TOO_LONG);
        assertThatThrownBy(() -> access.hashOptional("😀".repeat(20)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(RoomLockedException.TOO_LONG);
    }

    @Test
    void openRoomDoesNotAskForPassword() {
        UUID roomId = UUID.randomUUID();
        when(pokerRooms.findById(roomId)).thenReturn(OptionalOf(null));

        access.admitPoker(roomId, UUID.randomUUID(), null);

        verify(pokerMembers, never()).grant(any(), any());
    }

    @Test
    void memberSkipsPassword() {
        UUID roomId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        when(pokerRooms.findById(roomId)).thenReturn(OptionalOf(hasher.hash("secret")));
        when(pokerMembers.existsById(nullable(PokerRoomMemberEntity.Pk.class))).thenReturn(true);

        access.admitPoker(roomId, playerId, null);

        verify(pokerMembers, never()).grant(any(), any());
    }

    @Test
    void missingOrWrongPasswordDoesNotGrantAccess() {
        UUID roomId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        when(pokerRooms.findById(roomId)).thenReturn(OptionalOf(hasher.hash("secret")));
        when(pokerMembers.existsById(nullable(PokerRoomMemberEntity.Pk.class))).thenReturn(false);

        assertThatThrownBy(() -> access.admitPoker(roomId, playerId, "  "))
                .isInstanceOf(RoomLockedException.class)
                .hasMessage(RoomLockedException.REQUIRED);
        assertThatThrownBy(() -> access.admitPoker(roomId, playerId, "nope"))
                .isInstanceOf(RoomLockedException.class)
                .hasMessage(RoomLockedException.WRONG);
        verify(pokerMembers, never()).grant(any(), any());
    }

    @Test
    void correctPasswordAddsPlayerOnce() {
        UUID roomId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        when(retroRooms.findById(roomId)).thenReturn(retroRoom(hasher.hash("secret")));
        when(retroMembers.existsById(nullable(RetroRoomMemberEntity.Pk.class))).thenReturn(false);

        access.admitRetro(roomId, playerId, " secret ");

        verify(retroMembers).grant(roomId, playerId);
    }

    @Test
    void needsPasswordOnlyForLockedRoomsOutsideTheList() {
        UUID roomId = UUID.randomUUID();
        assertThat(RoomAccessUsecase.needsPassword(false, roomId, Set.of())).isFalse();
        assertThat(RoomAccessUsecase.needsPassword(true, roomId, Set.of(roomId))).isFalse();
        assertThat(RoomAccessUsecase.needsPassword(true, roomId, Set.of())).isTrue();
    }

    private static java.util.Optional<PokerRoomEntity> OptionalOf(String passwordHash) {
        PokerRoomEntity room = new PokerRoomEntity();
        room.setPasswordHash(passwordHash);
        return java.util.Optional.of(room);
    }

    private static java.util.Optional<RetroRoomEntity> retroRoom(String passwordHash) {
        RetroRoomEntity room = new RetroRoomEntity();
        room.setPasswordHash(passwordHash);
        return java.util.Optional.of(room);
    }
}
