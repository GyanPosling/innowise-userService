package com.innowise.userservice.security;

import com.innowise.userservice.exception.ResourceNotFoundException;
import com.innowise.userservice.repository.PaymentCardRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityUtilTest {

    private static final UUID AUTH_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID OTHER_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000011");

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
        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> securityUtil.getAuthenticatedUserId()
        );

        assertTrue(exception.getMessage().contains("Unauthenticated"));
    }

    @Test
    void getAuthenticatedUserId_InvalidDetails_ThrowsException() {
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken("user", "password", "ROLE_USER");
        authentication.setDetails(12345);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> securityUtil.getAuthenticatedUserId()
        );

        assertTrue(exception.getMessage().contains("Invalid security context"));
    }

    @Test
    void getAuthenticatedUserId_StringDetails_ReturnsUuid() {
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken("user", "password", "ROLE_USER");
        authentication.setDetails(AUTH_USER_ID.toString());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UUID result = securityUtil.getAuthenticatedUserId();

        assertEquals(AUTH_USER_ID, result);
    }

    @Test
    void checkOwnership_AdminBypassesOwnership() {
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken("admin", "password", "ROLE_ADMIN");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        securityUtil.checkOwnership(OTHER_USER_ID);

        verify(paymentCardRepository, never()).findUserIdByCardId(1);
    }

    @Test
    void checkOwnership_NotOwner_ThrowsException() {
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken("user", "password", "ROLE_USER");
        authentication.setDetails(AUTH_USER_ID.toString());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> securityUtil.checkOwnership(OTHER_USER_ID)
        );

        assertTrue(exception.getMessage().contains("Access Denied"));
    }

    @Test
    void isCardOwner_Admin_ReturnsTrue() {
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken("admin", "password", "ROLE_ADMIN");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertTrue(securityUtil.isCardOwner(1));
        verify(paymentCardRepository, never()).findUserIdByCardId(1);
    }

    @Test
    void isCardOwner_UserOwnsCard_ReturnsTrue() {
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken("user", "password", "ROLE_USER");
        authentication.setDetails(AUTH_USER_ID.toString());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(paymentCardRepository.findUserIdByCardId(1)).thenReturn(Optional.of(AUTH_USER_ID));

        assertTrue(securityUtil.isCardOwner(1));
    }

    @Test
    void isCardOwner_CardNotFound_ThrowsException() {
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken("user", "password", "ROLE_USER");
        authentication.setDetails(AUTH_USER_ID.toString());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(paymentCardRepository.findUserIdByCardId(1)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> securityUtil.isCardOwner(1)
        );

        assertTrue(exception.getMessage().contains("Card not found"));
    }
}
