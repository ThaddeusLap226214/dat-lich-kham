package com.clinic.auth.account;

import com.clinic.auth.account.dto.AccountResponse;
import com.clinic.auth.account.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Đăng nhập không đi qua đây: web chuyển sang trang đăng nhập của Keycloak và nhận token từ Keycloak
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AccountService accountService;

    public AuthController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse register(@Valid @RequestBody RegisterRequest request) {
        return accountService.register(request);
    }
}
