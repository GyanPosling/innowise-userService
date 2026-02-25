package com.innowise.userService.controller;

import com.innowise.userService.model.dto.UserDto;
import com.innowise.userService.security.SecurityUtil;
import com.innowise.userService.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "Users", description = "User management API")
public class UserController {

    private final UserService userService;
    private final SecurityUtil securityUtil;


    @Operation(summary = "Create a new user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserDto userDTO) {
        userDTO.setAuthUserId(securityUtil.getAuthenticatedUserId());
        UserDto createdUser = userService.createUser(userDTO);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    @Operation(summary = "Get user by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityUtil.getAuthenticatedUserId() == #id")
    public ResponseEntity<UserDto> getUser(
            @Parameter(description = "ID of the user", required = true, example = "1")
            @PathVariable Integer id) {
        UserDto user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @Operation(summary = "Get user by email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/by-email")
    @PreAuthorize("hasRole('ADMIN') or authentication.name.equalsIgnoreCase(#email)")
    public ResponseEntity<UserDto> getUserByEmail(
            @Parameter(description = "Email of the user", required = true, example = "user@example.com")
            @RequestParam String email) {
        UserDto user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }

    @Operation(summary = "Get users by emails")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users retrieved",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class)))
    })
    @PostMapping("/batch")
    @PreAuthorize("hasRole('ADMIN') or @securityUtil.isSelfEmailList(#emails)")
    public ResponseEntity<List<UserDto>> getUsersByEmails(
            @RequestBody List<String> emails) {
        return ResponseEntity.ok(userService.getUsersByEmails(emails));
    }

    @Operation(summary = "Get all users with filters and pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users retrieved")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserDto>> getAllUsers(
            @Parameter(description = "Filter by first name")
            @RequestParam(required = false) String firstName,

            @Parameter(description = "Filter by last name")
            @RequestParam(required = false) String lastName,

            @Parameter(description = "Pagination parameters")
            Pageable pageable) {
        Page<UserDto> users = userService.getAllUsers(firstName, lastName, pageable);
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Update user by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityUtil.getAuthenticatedUserId() == #id")
    public ResponseEntity<UserDto> updateUser(
            @Parameter(description = "ID of the user", required = true, example = "1")
            @PathVariable Integer id,

            @Valid @RequestBody UserDto userDTO) {
        UserDto updatedUser = userService.updateUser(id, userDTO);
        return ResponseEntity.ok(updatedUser);
    }

    @Operation(summary = "Toggle user status (active/inactive)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status toggled"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityUtil.getAuthenticatedUserId() == #id")
    public ResponseEntity<UserDto> toggleUserStatus(
            @Parameter(description = "ID of the user", required = true, example = "1")
            @PathVariable Integer id) {
        UserDto user = userService.toggleUserStatus(id);
        return ResponseEntity.ok(user);
    }

    @Operation(summary = "Delete user by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityUtil.getAuthenticatedUserId() == #id")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID of the user", required = true, example = "1")
            @PathVariable Integer id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
