package ru.bulbasaur.office.infra.rest.dto;

import java.util.List;

/** Готовые строки журнала событий для отображения на «принтере логов». */
public record LogsResponse(List<String> lines) {
}
