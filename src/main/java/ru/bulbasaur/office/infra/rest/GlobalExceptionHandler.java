package ru.bulbasaur.office.infra.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import ru.bulbasaur.office.usecase.exception.InvalidCredentialsException;
import ru.bulbasaur.office.usecase.exception.LoginAlreadyTakenException;
import ru.bulbasaur.office.usecase.exception.PlayerNotFoundException;
import ru.bulbasaur.office.usecase.exception.PokerRoomActiveException;
import ru.bulbasaur.office.usecase.exception.PokerRoomNotFoundException;
import ru.bulbasaur.office.usecase.exception.UnknownGameException;
import ru.bulbasaur.office.usecase.exception.UnknownQuestException;
import ru.bulbasaur.office.usecase.exception.WrongPasswordException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ApiError(String message) {
    }

    @ExceptionHandler(LoginAlreadyTakenException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleLoginTaken(LoginAlreadyTakenException e) {
        return new ApiError(e.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiError handleInvalidCredentials(InvalidCredentialsException e) {
        return new ApiError(e.getMessage());
    }

    @ExceptionHandler(UnknownGameException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleUnknownGame(UnknownGameException e) {
        return new ApiError(e.getMessage());
    }

    @ExceptionHandler(UnknownQuestException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleUnknownQuest(UnknownQuestException e) {
        return new ApiError(e.getMessage());
    }

    @ExceptionHandler(PlayerNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handlePlayerNotFound(PlayerNotFoundException e) {
        return new ApiError(e.getMessage());
    }

    @ExceptionHandler(PokerRoomNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handlePokerRoomNotFound(PokerRoomNotFoundException e) {
        return new ApiError(e.getMessage());
    }

    @ExceptionHandler(PokerRoomActiveException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handlePokerRoomActive(PokerRoomActiveException e) {
        return new ApiError(e.getMessage());
    }

    // Неверный старый пароль — 400, а не 401: клиент разлогинивается по 401/403,
    // а здесь сессия игрока валидна.
    @ExceptionHandler(WrongPasswordException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleWrongPassword(WrongPasswordException e) {
        return new ApiError(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleIllegalArgument(IllegalArgumentException e) {
        return new ApiError(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return new ApiError(message.isBlank() ? "Некорректный запрос" : message);
    }

    // Причина из ResponseStatusException иначе не попадает в JSON
    // (spring.web.error.include-message=never), и клиент показывает «Ошибка 400».
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleStatus(ResponseStatusException e) {
        String reason = e.getReason();
        if (reason == null || reason.isBlank()) {
            reason = fallback(e.getStatusCode());
        }
        return ResponseEntity.status(e.getStatusCode()).body(new ApiError(reason));
    }

    private static String fallback(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "Некорректный запрос.";
            case 404 -> "Не найдено.";
            case 409 -> "Конфликт.";
            default -> "Ошибка " + status.value();
        };
    }
}
