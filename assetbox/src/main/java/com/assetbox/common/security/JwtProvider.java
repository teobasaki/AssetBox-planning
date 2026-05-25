package com.assetbox.common.security;

import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

    public String createToken(Long userId, String email) {
        return "TODO";
    }

    public boolean validate(String token) {
        return token != null && !token.isBlank();
    }
}
