package ru.bulbasaur.office.infra.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.bulbasaur.office.infra.rest.dto.AuthResponse;
import ru.bulbasaur.office.infra.rest.dto.LoginRequest;
import ru.bulbasaur.office.infra.rest.dto.RegisterRequest;
import ru.bulbasaur.office.usecase.LoginUsecase;
import ru.bulbasaur.office.usecase.RegisterUsecase;
import ru.bulbasaur.office.usecase.dto.AuthResult;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterUsecase registerUsecase;
    private final LoginUsecase loginUsecase;

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        AuthResult result = registerUsecase.execute(request.login(), request.password());
        return new AuthResponse(result.token(), result.login());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        AuthResult result = loginUsecase.execute(request.login(), request.password());
        return new AuthResponse(result.token(), result.login());
    }
}
