package ru.bulbasaur.office.infra.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import ru.bulbasaur.office.infra.persistence.entity.RetroMemeEntity;
import ru.bulbasaur.office.infra.persistence.entity.RetroRoomEntity;
import ru.bulbasaur.office.infra.rest.dto.RetroMemeUploadRequest;
import ru.bulbasaur.office.infra.rest.dto.RetroMemeUploadResponse;
import ru.bulbasaur.office.infra.security.AuthPrincipal;
import ru.bulbasaur.office.infra.ws.dto.RetroStateOut;
import ru.bulbasaur.office.infra.ws.handler.RetroWsHandler;
import ru.bulbasaur.office.usecase.retro.AddRetroMemeUsecase;
import ru.bulbasaur.office.usecase.retro.BuildRetroStateUsecase;
import ru.bulbasaur.office.usecase.retro.CloseRetroRoomUsecase;
import ru.bulbasaur.office.usecase.retro.GetRetroMemeUsecase;
import ru.bulbasaur.office.usecase.retro.GetRetroRoomsUsecase;
import ru.bulbasaur.office.usecase.retro.RetroResult;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/retro")
@RequiredArgsConstructor
public class RetroController {

    private final GetRetroRoomsUsecase getRooms;
    private final CloseRetroRoomUsecase closeRoom;
    private final AddRetroMemeUsecase addMeme;
    private final GetRetroMemeUsecase getMeme;
    private final BuildRetroStateUsecase buildState;
    private final RetroWsHandler retroWs;

    @PostMapping("/rooms/{roomId}/memes")
    public RetroMemeUploadResponse uploadMeme(
            @PathVariable UUID roomId,
            @RequestBody RetroMemeUploadRequest body,
            @AuthenticationPrincipal AuthPrincipal player
    ) {
        RetroRoomEntity room = getRooms.findRoom(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Комната не найдена"));
        if (!RetroRoomEntity.STATUS_ACTIVE.equals(room.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ретро уже завершено");
        }
        if (!retroWs.isParticipant(roomId, player.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Нужно сначала войти в комнату");
        }
        byte[] data;
        try {
            data = Base64.getDecoder().decode(body.dataBase64());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Некорректный Base64");
        }
        RetroResult<UUID> result = addMeme.execute(roomId, player.id(), body.mimeType(), data);
        if (!result.ok()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, result.error());
        }
        retroWs.broadcastRoom(roomId);
        return new RetroMemeUploadResponse(result.value().toString(), "/api/retro/memes/" + result.value());
    }

    @GetMapping("/memes/{memeId}")
    public ResponseEntity<byte[]> getMeme(
            @PathVariable UUID memeId,
            @AuthenticationPrincipal AuthPrincipal player
    ) {
        RetroMemeEntity meme = getMeme.execute(memeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(meme.getMimeType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok().contentType(mediaType).body(meme.getImageData());
    }

    @GetMapping("/rooms/{roomId}")
    public RetroStateOut getRoom(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal AuthPrincipal player
    ) {
        RetroRoomEntity room = getRooms.findRoom(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Комната не найдена"));
        boolean readOnly = RetroRoomEntity.STATUS_CLOSED.equals(room.getStatus())
                || room.getClosesAt().toEpochMilli() <= System.currentTimeMillis();
        if (RetroRoomEntity.STATUS_ACTIVE.equals(room.getStatus())
                && room.getClosesAt().toEpochMilli() <= System.currentTimeMillis()) {
            closeRoom.execute(roomId);
            readOnly = true;
        }
        long remaining = readOnly ? 0 : Math.max(0, room.getClosesAt().toEpochMilli() - System.currentTimeMillis());
        return buildState.execute(room, player.id(), List.of(), remaining, true);
    }
}
