package ru.bulbasaur.office.infra.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record SaveRoleRequest(@NotBlank String role) {
}
