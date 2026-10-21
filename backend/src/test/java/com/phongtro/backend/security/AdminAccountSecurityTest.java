package com.phongtro.backend.security;

import com.phongtro.backend.entity.*;
import com.phongtro.backend.exception.AppException;
import com.phongtro.backend.mapper.UserMapper;
import com.phongtro.backend.repository.UserRepository;
import com.phongtro.backend.service.impl.UserServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminAccountSecurityTest {
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void bannedOrInactiveUserCannotKeepUsingExistingJwt() throws Exception {
        for (UserStatus status : new UserStatus[]{UserStatus.BANNED, UserStatus.INACTIVE}) {
            JwtTokenProvider tokens = mock(JwtTokenProvider.class);
            UserDetailsServiceImpl details = mock(UserDetailsServiceImpl.class);
            when(tokens.validateToken("old-token")).thenReturn(true);
            when(tokens.getEmailFromToken("old-token")).thenReturn("tenant@test.local");
            when(details.loadUserByUsername("tenant@test.local")).thenReturn(UserPrincipal.create(User.builder()
                    .id(UUID.randomUUID()).email("tenant@test.local").role(Role.TENANT).status(status).build()));
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer old-token");
            new JwtAuthenticationFilter(tokens, details).doFilter(request, new MockHttpServletResponse(),
                    (req, res) -> assertNull(SecurityContextHolder.getContext().getAuthentication()));
        }
    }
    @Test void adminAccountCannotBeBanned() {
        UserRepository users = mock(UserRepository.class);
        UUID id = UUID.randomUUID();
        when(users.findById(id)).thenReturn(Optional.of(User.builder().id(id).role(Role.ADMIN).status(UserStatus.ACTIVE).build()));
        UserServiceImpl service = new UserServiceImpl(users, mock(UserMapper.class), mock(PasswordEncoder.class));
        assertThrows(AppException.class, () -> service.updateUserStatus(id, UserStatus.BANNED));
        verify(users, never()).save(any(User.class));
    }
}
