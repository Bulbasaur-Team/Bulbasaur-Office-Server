package ru.bulbasaur.office.infra.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.bulbasaur.office.domain.model.Role;
import ru.bulbasaur.office.infra.rest.dto.ChangePasswordRequest;
import ru.bulbasaur.office.infra.rest.dto.ProfileResponse;
import ru.bulbasaur.office.infra.rest.dto.SaveRoleRequest;
import ru.bulbasaur.office.infra.security.AuthPrincipal;
import ru.bulbasaur.office.usecase.ChangePasswordUsecase;
import ru.bulbasaur.office.usecase.DeleteAccountUsecase;
import ru.bulbasaur.office.usecase.GetProfileUsecase;
import ru.bulbasaur.office.usecase.SaveRoleUsecase;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final DeleteAccountUsecase deleteAccountUsecase;
    private final GetProfileUsecase getProfileUsecase;
    private final SaveRoleUsecase saveRoleUsecase;
    private final ChangePasswordUsecase changePasswordUsecase;

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthPrincipal player) {
        deleteAccountUsecase.execute(player.id());
    }

    @GetMapping("/profile")
    public ProfileResponse profile(@AuthenticationPrincipal AuthPrincipal player) {
        return new ProfileResponse(
                player.login(),
                getProfileUsecase.execute(player.id()).map(Role::name).orElse(null));
    }

    @PutMapping("/role")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveRole(@Valid @RequestBody SaveRoleRequest request,
                         @AuthenticationPrincipal AuthPrincipal player) {
        Role role = Role.fromName(request.role())
                .orElseThrow(() -> new IllegalArgumentException("Неизвестная роль: " + request.role()));
        saveRoleUsecase.execute(player.id(), role);
    }

    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request,
                               @AuthenticationPrincipal AuthPrincipal player) {
        changePasswordUsecase.execute(player.id(), request.oldPassword(), request.newPassword());
    }
}
