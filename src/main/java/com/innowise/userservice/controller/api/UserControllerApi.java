package com.innowise.userservice.controller.api;

import com.innowise.userservice.model.dto.InternalUserCreateRequest;
import com.innowise.userservice.model.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

@Tag(name = "Users", description = "User management endpoints")
@SecurityRequirement(name = "bearerAuth")
public interface UserControllerApi {

    @Operation(summary = "Create user profile for the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created",
                    content = @Content(schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "409", description = "User already exists")
    })
    ResponseEntity<UserDto> createUser(
            @Valid
            @RequestBody(required = true, description = "User payload",
                    content = @Content(schema = @Schema(implementation = UserDto.class)))
            UserDto userDTO);

    @Operation(summary = "Create user from internal service request")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    ResponseEntity<Void> createInternalUser(
            @Valid
            @RequestBody(required = true, description = "Internal user payload",
                    content = @Content(schema = @Schema(implementation = InternalUserCreateRequest.class)))
            InternalUserCreateRequest request);

    @Operation(summary = "Get user by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found",
                    content = @Content(schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    ResponseEntity<UserDto> getUser(
            @Parameter(description = "User id") UUID id);

    @Operation(summary = "Get user by email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found",
                    content = @Content(schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    ResponseEntity<UserDto> getUserByEmail(
            @Parameter(description = "User email") String email);

    @Operation(summary = "Get users by email list")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users found",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserDto.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    ResponseEntity<List<UserDto>> getUsersByEmails(
            @RequestBody(required = true, description = "User emails")
            List<String> emails);

    @Operation(summary = "Get paged users list")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    ResponseEntity<Page<UserDto>> getAllUsers(
            @Parameter(description = "First name filter") String firstName,
            @Parameter(description = "Last name filter") String lastName,
            Pageable pageable);

    @Operation(summary = "Update user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated",
                    content = @Content(schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    ResponseEntity<UserDto> updateUser(
            @Parameter(description = "User id") UUID id,
            @Valid
            @RequestBody(required = true, description = "Updated user payload",
                    content = @Content(schema = @Schema(implementation = UserDto.class)))
            UserDto userDTO);

    @Operation(summary = "Toggle user active status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User status toggled",
                    content = @Content(schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    ResponseEntity<UserDto> toggleUserStatus(
            @Parameter(description = "User id") UUID id);

    @Operation(summary = "Delete user")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deleted"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    ResponseEntity<Void> deleteUser(
            @Parameter(description = "User id") UUID id);
}
