package com.innowise.userservice.security;

import com.innowise.userservice.exception.ResourceNotFoundException;
import com.innowise.userservice.repository.PaymentCardRepository;
import java.util.Objects;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtil {

    private final JwtUtil jwtUtil;
    private final PaymentCardRepository paymentCardRepository;

    public Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Unauthenticated access attempt");
        }

        Object credentials = authentication.getCredentials();
        if (!(credentials instanceof String token)) {
            throw new AccessDeniedException("Invalid security context: missing token");
        }

        return jwtUtil.extractUserId(token);
    }

    public void checkOwnership(Long resourceOwnerId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Unauthenticated access attempt");
        }

        boolean isAdmin = isAdmin(authentication);

        if (isAdmin) {
            return;
        }

        Long currentUserId = getAuthenticatedUserId();
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
        Integer ownerId = paymentCardRepository.findUserIdByCardId(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + cardId));
        Long currentUserId = getAuthenticatedUserId();
        return currentUserId != null && currentUserId.equals(ownerId.longValue());
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
}
