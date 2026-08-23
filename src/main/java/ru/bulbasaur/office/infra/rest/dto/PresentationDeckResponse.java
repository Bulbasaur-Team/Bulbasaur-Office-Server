package ru.bulbasaur.office.infra.rest.dto;

import java.util.List;

public record PresentationDeckResponse(List<String> slideIds, String status) {
}
