package com.clinic.auth.account;

import com.clinic.auth.account.dto.AccountResponse;
import com.clinic.auth.account.dto.CreateAccountRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/me")
    public AccountResponse me(@RequestHeader("X-User-Id") Long accountId) {
        return accountService.getById(accountId);
    }

    @GetMapping
    public List<AccountResponse> list(@RequestParam(required = false) Role role) {
        return accountService.list(role);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody CreateAccountRequest request,
                                  @RequestHeader("X-User-Role") String callerRole) {
        return accountService.create(request, callerRole);
    }
}
