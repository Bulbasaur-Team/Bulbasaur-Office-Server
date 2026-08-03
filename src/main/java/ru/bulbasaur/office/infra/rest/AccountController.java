package ru.bulbasaur.office.infra.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.bulbasaur.office.infra.rest.dto.AuthResponse;
import ru.bulbasaur.office.infra.rest.dto.BulbaCoinHistoryResponse;
import ru.bulbasaur.office.infra.rest.dto.ChangePasswordRequest;
import ru.bulbasaur.office.infra.rest.dto.ProfileResponse;
import ru.bulbasaur.office.infra.security.AuthPrincipal;
import ru.bulbasaur.office.usecase.ChangePasswordUsecase;
import ru.bulbasaur.office.usecase.DeleteAccountUsecase;
import ru.bulbasaur.office.usecase.GetProfileUsecase;
import ru.bulbasaur.office.usecase.ListBulbaCoinTransactionsUsecase;
import ru.bulbasaur.office.usecase.RefreshTokenUsecase;
import ru.bulbasaur.office.usecase.dto.AuthResult;
import ru.bulbasaur.office.usecase.dto.BulbaCoinHistoryView;
import ru.bulbasaur.office.usecase.dto.ProfileView;

import java.time.Instant;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final DeleteAccountUsecase deleteAccountUsecase;
    private final GetProfileUsecase getProfileUsecase;
    private final ChangePasswordUsecase changePasswordUsecase;
    private final RefreshTokenUsecase refreshTokenUsecase;
    private final ListBulbaCoinTransactionsUsecase listBulbaCoinTransactions;

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthPrincipal player) {
        deleteAccountUsecase.execute(player.id());
    }

    /** Продлить сессию: новый JWT с тем же TTL от текущего момента. */
    @PostMapping("/refresh")
    public AuthResponse refresh(@AuthenticationPrincipal AuthPrincipal player) {
        AuthResult result = refreshTokenUsecase.execute(player.id(), player.login());
        return new AuthResponse(result.token(), result.login());
    }

    @GetMapping("/profile")
    public ProfileResponse profile(@AuthenticationPrincipal AuthPrincipal player) {
        ProfileView view = getProfileUsecase.execute(player.id());
        return new ProfileResponse(view.login(), view.bulbaCoinBalance(), view.appearance());
    }

    @GetMapping("/bulba-coins/transactions")
    public BulbaCoinHistoryResponse transactions(
            @AuthenticationPrincipal AuthPrincipal player,
            @RequestParam(required = false) Instant before,
            @RequestParam(defaultValue = "50") int limit) {
        BulbaCoinHistoryView view = listBulbaCoinTransactions.execute(player.id(), before, limit);
        return new BulbaCoinHistoryResponse(
                view.balance(),
                view.transactions().stream()
                        .map(t -> new BulbaCoinHistoryResponse.Transaction(
                                t.id(), t.amount(), t.kind(), t.title(), t.createdAt()))
                        .toList());
    }

    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request,
                               @AuthenticationPrincipal AuthPrincipal player) {
        changePasswordUsecase.execute(player.id(), request.oldPassword(), request.newPassword());
    }
}
