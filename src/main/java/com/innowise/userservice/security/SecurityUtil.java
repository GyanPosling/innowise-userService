package com.innowise.userservice.security;

import com.innowise.userservice.exception.ResourceNotFoundException;
import com.innowise.userservice.repository.PaymentCardRepository;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@RequiredArgsConstructor
public class SecurityUtil {

    private final PaymentCardRepository paymentCardRepository;

    @Value("${security.internal.secret}")
    private String internalSecret;

    public UUID getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Unauthenticated access attempt");
        }

        Object details = authentication.getDetails();
        if (details instanceof UUID userId) {
            return userId;
        }

        if (details instanceof String stringId) {
            try {
                return UUID.fromString(stringId);
            } catch (IllegalArgumentException ex) {
                throw new AccessDeniedException("Invalid security context: malformed user id");
            }
        }

        throw new AccessDeniedException("Invalid security context: missing user id");
    }

    public String getAuthenticatedEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Unauthenticated access attempt");
        }
        return authentication.getName();
    }

    public void checkOwnership(UUID resourceOwnerId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Unauthenticated access attempt");
        }

        boolean isAdmin = isAdmin(authentication);

        if (isAdmin) {
            return;
        }

        UUID currentUserId = getAuthenticatedUserId();
        if (!Objects.equals(currentUserId, resourceOwnerId)) {
            throw new AccessDeniedException("Access Denied: You do not own this resource.");
        }
    }

    public boolean isCardOwner(Integer cardId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Unauthenticated access attempt");
        }
        if (isAdmin(authentication)) {
            return true;
        }
        UUID ownerId = paymentCardRepository.findUserIdByCardId(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + cardId));
        UUID currentUserId = getAuthenticatedUserId();
        return currentUserId != null && currentUserId.equals(ownerId);
    }

    public boolean isSelfEmailList(Collection<String> emails) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String authEmail = authentication.getName();
        if (authEmail == null) {
            return false;
        }
        return emails != null
                && !emails.isEmpty()
                && emails.stream().allMatch(email -> authEmail.equalsIgnoreCase(email));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));
    }

    public boolean isInternalRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return false;
        }
        String headerValue = attributes.getRequest().getHeader("X-Internal-Secret");
        return internalSecret != null
                && !internalSecret.isBlank()
                && internalSecret.equals(headerValue);
    }
}
