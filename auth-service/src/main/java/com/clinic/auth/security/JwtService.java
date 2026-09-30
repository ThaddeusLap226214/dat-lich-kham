package com.clinic.auth.security;

import com.clinic.auth.account.Account;
import com.clinic.auth.common.NotImplementedYetException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final String secret;
    private final long expirationMinutes;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration-minutes}") long expirationMinutes) {
        this.secret = secret;
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(Account account) {
        throw new NotImplementedYetException("tạo JWT HS256: sub = id tài khoản, claim username, role, doctorId");
    }

    public long getExpirationSeconds() {
        return expirationMinutes * 60;
    }
}
