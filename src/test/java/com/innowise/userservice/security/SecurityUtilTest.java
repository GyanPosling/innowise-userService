package com.innowise.userservice.security;

import com.innowise.userservice.exception.ResourceNotFoundException;
import com.innowise.userservice.repository.PaymentCardRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.TestingAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityUtilTest {

    @Mock
    private PaymentCardRepository paymentCardRepository;

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
    void getAuthenticatedUserId_InvalidDetails_ThrowsException() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                "user",
                "password",
                "ROLE_USER"
        );
        authentication.setAuthenticated(true);
        authentication.setDetails(12345);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> securityUtil.getAuthenticatedUserId());

        assertTrue(exception.getMessage().contains("Invalid security context"));
    }

    @Test
    void getAuthenticatedUserId_Success() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                "user",
                "password",
                "ROLE_USER"
        );
        authentication.setAuthenticated(true);
        authentication.setDetails("42");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Long result = securityUtil.getAuthenticatedUserId();

        assertEquals(42L, result);
    }

    @Test
    void checkOwnership_AdminBypassesOwnership() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                "admin",
                "password",
                "ROLE_ADMIN"
        );
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        securityUtil.checkOwnership(99L);
    }

    @Test
    void checkOwnership_NotOwner_ThrowsException() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                "user",
                "password",
                "ROLE_USER"
        );
        authentication.setAuthenticated(true);
        authentication.setDetails("10");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> securityUtil.checkOwnership(11L));

        assertTrue(exception.getMessage().contains("Access Denied"));
    }

    @Test
    void isCardOwner_Admin_ReturnsTrue() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                "admin",
                "password",
                "ROLE_ADMIN"
        );
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        boolean result = securityUtil.isCardOwner(1);

        assertTrue(result);
        verify(paymentCardRepository, never()).findUserIdByCardId(1);
    }

    @Test
    void isCardOwner_UserOwnsCard_ReturnsTrue() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                "user",
                "password",
                "ROLE_USER"
        );
        authentication.setAuthenticated(true);
        authentication.setDetails("10");
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(paymentCardRepository.findUserIdByCardId(1)).thenReturn(Optional.of(10));

        boolean result = securityUtil.isCardOwner(1);

        assertTrue(result);
        verify(paymentCardRepository).findUserIdByCardId(1);
    }

    @Test
    void isCardOwner_CardNotFound_ThrowsException() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                "user",
                "password",
                "ROLE_USER"
        );
        authentication.setAuthenticated(true);
        authentication.setDetails("10");
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(paymentCardRepository.findUserIdByCardId(1)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> securityUtil.isCardOwner(1));

        assertTrue(exception.getMessage().contains("Card not found"));
    }

}
