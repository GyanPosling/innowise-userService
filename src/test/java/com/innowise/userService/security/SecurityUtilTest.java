package com.innowise.userservice.security;

import com.innowise.userservice.exception.ResourceNotFoundException;
import com.innowise.userservice.repository.PaymentCardRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityUtilTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PaymentCardRepository paymentCardRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SecurityUtil securityUtil;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAuthenticatedUserId_Unauthenticated_ThrowsException() {
        SecurityContextHolder.clearContext();

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> securityUtil.getAuthenticatedUserId());

        assertTrue(exception.getMessage().contains("Unauthenticated access attempt"));
    }

    @Test
    void getAuthenticatedUserId_InvalidToken_ThrowsException() {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getCredentials()).thenReturn(12345);

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> securityUtil.getAuthenticatedUserId());

        assertTrue(exception.getMessage().contains("Invalid security context"));
    }

    @Test
    void getAuthenticatedUserId_Success() {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getCredentials()).thenReturn("token");
        when(jwtUtil.extractUserId("token")).thenReturn(42L);

        Long result = securityUtil.getAuthenticatedUserId();

        assertEquals(42L, result);
        verify(jwtUtil).extractUserId("token");
    }

    @Test
    void checkOwnership_AdminBypassesOwnership() {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        doReturn(authorities("ROLE_ADMIN")).when(authentication).getAuthorities();

        securityUtil.checkOwnership(99L);

        verify(jwtUtil, never()).extractUserId("token");
    }

    @Test
    void checkOwnership_NotOwner_ThrowsException() {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        doReturn(authorities("ROLE_USER")).when(authentication).getAuthorities();
        when(authentication.getCredentials()).thenReturn("token");
        when(jwtUtil.extractUserId("token")).thenReturn(10L);

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> securityUtil.checkOwnership(11L));

        assertTrue(exception.getMessage().contains("Access Denied"));
    }

    @Test
    void isCardOwner_Admin_ReturnsTrue() {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        doReturn(authorities("ROLE_ADMIN")).when(authentication).getAuthorities();

        boolean result = securityUtil.isCardOwner(1);

        assertTrue(result);
        verify(paymentCardRepository, never()).findUserIdByCardId(1);
    }

    @Test
    void isCardOwner_UserOwnsCard_ReturnsTrue() {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        doReturn(authorities("ROLE_USER")).when(authentication).getAuthorities();
        when(paymentCardRepository.findUserIdByCardId(1)).thenReturn(Optional.of(10));
        when(authentication.getCredentials()).thenReturn("token");
        when(jwtUtil.extractUserId("token")).thenReturn(10L);

        boolean result = securityUtil.isCardOwner(1);

        assertTrue(result);
        verify(paymentCardRepository).findUserIdByCardId(1);
    }

    @Test
    void isCardOwner_CardNotFound_ThrowsException() {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        doReturn(authorities("ROLE_USER")).when(authentication).getAuthorities();
        when(paymentCardRepository.findUserIdByCardId(1)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> securityUtil.isCardOwner(1));

        assertTrue(exception.getMessage().contains("Card not found"));
    }

    private static Collection<? extends GrantedAuthority> authorities(String role) {
        return List.of(new SimpleGrantedAuthority(role));
    }
}
