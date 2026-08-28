package ru.bulbasaur.office.usecase.dto;

import java.util.List;

public record PresentationDeckView(List<String> slideIds, String status) {
}
