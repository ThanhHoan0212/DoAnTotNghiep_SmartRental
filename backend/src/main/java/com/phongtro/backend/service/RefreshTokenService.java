package com.phongtro.backend.service;

import com.phongtro.backend.entity.RefreshToken;
import com.phongtro.backend.entity.User;

import java.util.Optional;

public interface RefreshTokenService {

    RefreshToken createRefreshToken(User user);

    RefreshToken verifyExpiration(RefreshToken token);

    Optional<RefreshToken> findByToken(String token);

    void revokeUserTokens(User user);

    void revokeToken(String token);
}
