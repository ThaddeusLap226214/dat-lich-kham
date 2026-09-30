package com.clinic.auth.account;

import com.clinic.auth.account.dto.AccountResponse;
import com.clinic.auth.account.dto.CreateAccountRequest;
import com.clinic.auth.account.dto.LoginRequest;
import com.clinic.auth.account.dto.LoginResponse;
import com.clinic.auth.account.dto.RegisterRequest;
import com.clinic.auth.common.NotImplementedYetException;
import com.clinic.auth.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AccountService(AccountRepository accountRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AccountResponse register(RegisterRequest request) {
        throw new NotImplementedYetException("đăng ký tài khoản PATIENT, băm mật khẩu, 409 USERNAME_TAKEN nếu trùng");
    }

    public LoginResponse login(LoginRequest request) {
        throw new NotImplementedYetException("kiểm tra mật khẩu, cấp JWT, 401 INVALID_CREDENTIALS nếu sai");
    }

    public AccountResponse getById(Long id) {
        throw new NotImplementedYetException("lấy thông tin tài khoản");
    }

    public List<AccountResponse> list(Role role) {
        throw new NotImplementedYetException("danh sách tài khoản, lọc theo vai trò");
    }

    public AccountResponse create(CreateAccountRequest request, String callerRole) {
        throw new NotImplementedYetException("ADMIN tạo tài khoản RECEPTIONIST/DOCTOR/ADMIN");
    }
}
