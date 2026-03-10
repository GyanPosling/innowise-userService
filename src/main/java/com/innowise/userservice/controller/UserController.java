package com.innowise.userservice.controller;

import com.innowise.userservice.controller.api.UserControllerApi;
import com.innowise.userservice.model.dto.InternalUserCreateRequest;
import com.innowise.userservice.model.dto.UserDto;
import com.innowise.userservice.security.SecurityUtil;
import com.innowise.userservice.service.UserService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
public class UserController implements UserControllerApi {

    private final UserService userService;
    private final SecurityUtil securityUtil;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Override
    public ResponseEntity<UserDto> createUser(@RequestBody UserDto userDTO) {
        userDTO.setId(securityUtil.getAuthenticatedUserId());
        userDTO.setEmail(securityUtil.getAuthenticatedEmail());
        UserDto createdUser = userService.createUser(userDTO);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    @PostMapping("/internal")
    @PreAuthorize("@securityUtil.isInternalRequest()")
    @Override
    public ResponseEntity<Void> createInternalUser(@RequestBody InternalUserCreateRequest request) {
        userService.createUser(UserDto.builder()
                .id(request.getId())
                .name(request.getName())
                .surname(request.getSurname())
                .birthDate(request.getBirthDate())
                .email(request.getEmail())
                .active(true)
                .build());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityUtil.getAuthenticatedUserId() == #id")
    @Override
    public ResponseEntity<UserDto> getUser(
            @PathVariable UUID id) {
        UserDto user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/by-email")
    @PreAuthorize("hasRole('ADMIN') or authentication.name.equalsIgnoreCase(#email)")
    @Override
    public ResponseEntity<UserDto> getUserByEmail(
            @RequestParam String email) {
        UserDto user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/batch")
    @PreAuthorize("hasRole('ADMIN') or @securityUtil.isSelfEmailList(#emails)")
    @Override
    public ResponseEntity<List<UserDto>> getUsersByEmails(
            @RequestBody List<String> emails) {
        return ResponseEntity.ok(userService.getUsersByEmails(emails));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<Page<UserDto>> getAllUsers(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            Pageable pageable) {
        Page<UserDto> users = userService.getAllUsers(firstName, lastName, pageable);
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityUtil.getAuthenticatedUserId() == #id")
    @Override
    public ResponseEntity<UserDto> updateUser(
            @PathVariable UUID id,
            @RequestBody UserDto userDTO) {
        UserDto updatedUser = userService.updateUser(id, userDTO);
        return ResponseEntity.ok(updatedUser);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityUtil.getAuthenticatedUserId() == #id")
    @Override
    public ResponseEntity<UserDto> toggleUserStatus(
            @PathVariable UUID id) {
        UserDto user = userService.toggleUserStatus(id);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@securityUtil.isInternalRequest() or hasRole('ADMIN') or @securityUtil.getAuthenticatedUserId() == #id")
    @Override
    public ResponseEntity<Void> deleteUser(
            @PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
