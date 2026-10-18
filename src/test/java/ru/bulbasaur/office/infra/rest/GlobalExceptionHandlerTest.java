package ru.bulbasaur.office.infra.rest;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void statusExceptionKeepsReasonForTheClient() {
        ResponseEntity<GlobalExceptionHandler.ApiError> response = handler.handleStatus(
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Картинка весит 4,8 МБ. Максимум — 2 МБ, сожмите её или выберите другую."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("4,8 МБ").contains("2 МБ");
    }

    @Test
    void statusExceptionWithoutReasonUsesFallback() {
        ResponseEntity<GlobalExceptionHandler.ApiError> response = handler.handleStatus(
                new ResponseStatusException(HttpStatus.NOT_FOUND));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Не найдено.");
    }
}
