package com.innowise.userservice.controller;

import com.innowise.userservice.model.dto.InternalUserAuthLinkRequest;
import com.innowise.userservice.model.dto.InternalUserCreateRequest;
import com.innowise.userservice.model.dto.InternalUserCreateResponse;
import com.innowise.userservice.model.dto.UserDto;
import com.innowise.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

    private final UserService userService;

    @Value("${userservice.internal-endpoint-secret}")
    private String internalSecret;

    @PostMapping
    public ResponseEntity<InternalUserCreateResponse> createInternalUser(
            @RequestHeader(INTERNAL_SECRET_HEADER) String secret,
            @Valid @RequestBody InternalUserCreateRequest request
    ) {
        if (!isSecretValid(secret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        UserDto createdUser = userService.createInternalUser(request);
        InternalUserCreateResponse response = InternalUserCreateResponse.builder()
                .userId(createdUser.getId())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{userId}/auth")
    public ResponseEntity<Void> linkAuthUser(
            @RequestHeader(INTERNAL_SECRET_HEADER) String secret,
            @PathVariable Integer userId,
            @Valid @RequestBody InternalUserAuthLinkRequest request
    ) {
        if (!isSecretValid(secret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        userService.linkAuthUserId(userId, request.getAuthUserId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> rollbackInternalUser(
            @RequestHeader(INTERNAL_SECRET_HEADER) String secret,
            @PathVariable Integer userId
    ) {
        if (!isSecretValid(secret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        userService.deleteInternalUser(userId);
        return ResponseEntity.noContent().build();
    }

    private boolean isSecretValid(String secret) {
        return internalSecret != null
                && !internalSecret.isBlank()
                && internalSecret.equals(secret);
    }

}
