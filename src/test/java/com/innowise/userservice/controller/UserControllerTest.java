package com.innowise.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.userservice.AbstractIntegrationTest;
import com.innowise.userservice.model.dto.InternalUserCreateRequest;
import com.innowise.userservice.model.dto.UserDto;
import com.innowise.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
@AutoConfigureMockMvc
class UserControllerTest extends AbstractIntegrationTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void createUser_AuthenticatedRequest_UsesGatewayIdentity() throws Exception {
        UUID userId = testUserId(1);
        UserDto request = UserDto.builder()
                .name("John")
                .surname("Doe")
                .email("payload@example.com")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

        mockMvc.perform(post("/api/users")
                        .headers(userHeaders(userId, "john.doe@example.com", "POST", "/api/users"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(userId.toString())))
                .andExpect(jsonPath("$.email", is("john.doe@example.com")))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    void createInternalUser_ValidRequest_ReturnsCreated() throws Exception {
        UUID userId = createInternalUser(testUserId(2), "john.doe@example.com");

        mockMvc.perform(get("/api/users/{id}", userId)
                        .headers(adminHeaders("GET", "/api/users/" + userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(userId.toString())))
                .andExpect(jsonPath("$.name", is("John")))
                .andExpect(jsonPath("$.email", is("john.doe@example.com")))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    void createInternalUser_DuplicateEmail_ReturnsBadRequest() throws Exception {
        createInternalUser(testUserId(3), "john.doe@example.com");

        InternalUserCreateRequest duplicateRequest = buildInternalUser(testUserId(4), "john.doe@example.com");

        mockMvc.perform(post("/api/users/internal")
                        .headers(internalHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("email")));
    }

    @Test
    void createUser_InvalidInput_ReturnsBadRequest() throws Exception {
        UUID userId = testUserId(5);
        UserDto request = UserDto.builder()
                .name("")
                .surname("D")
                .email("bad-email")
                .birthDate(LocalDate.now().plusDays(1))
                .build();

        mockMvc.perform(post("/api/users")
                        .headers(userHeaders(userId, "john.doe@example.com", "POST", "/api/users"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("name")))
                .andExpect(jsonPath("$.message", containsString("surname")))
                .andExpect(jsonPath("$.message", containsString("email")))
                .andExpect(jsonPath("$.message", containsString("birthDate")));
    }

    @Test
    void getAllUsers_WithPagination_ReturnsPage() throws Exception {
        createInternalUser(testUserId(6), "user6@example.com");
        createInternalUser(testUserId(7), "user7@example.com");
        createInternalUser(testUserId(8), "user8@example.com");

        mockMvc.perform(get("/api/users")
                        .headers(adminHeaders("GET", "/api/users"))
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", is(3)));
    }

    @Test
    void updateUser_ValidInput_ReturnsUpdatedUser() throws Exception {
        UUID userId = createInternalUser(testUserId(9), "john.doe@example.com");
        UserDto updateDto = UserDto.builder()
                .name("Jane")
                .surname("Smith")
                .email("jane.smith@example.com")
                .birthDate(LocalDate.of(1992, 2, 2))
                .build();

        mockMvc.perform(put("/api/users/{id}", userId)
                        .headers(adminHeaders("PUT", "/api/users/" + userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Jane")))
                .andExpect(jsonPath("$.surname", is("Smith")))
                .andExpect(jsonPath("$.email", is("jane.smith@example.com")));
    }

    @Test
    void toggleUserStatus_UserExists_TogglesStatus() throws Exception {
        UUID userId = createInternalUser(testUserId(10), "john.doe@example.com");

        mockMvc.perform(patch("/api/users/{id}", userId)
                        .headers(adminHeaders("PATCH", "/api/users/" + userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(false)));
    }

    @Test
    void deleteUser_UserExists_ReturnsNoContent() throws Exception {
        UUID userId = createInternalUser(testUserId(11), "john.doe@example.com");

        mockMvc.perform(delete("/api/users/{id}", userId)
                        .headers(adminHeaders("DELETE", "/api/users/" + userId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/{id}", userId)
                        .headers(adminHeaders("GET", "/api/users/" + userId)))
                .andExpect(status().isNotFound());
    }

    private UUID createInternalUser(UUID userId, String email) throws Exception {
        mockMvc.perform(post("/api/users/internal")
                        .headers(internalHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildInternalUser(userId, email))))
                .andExpect(status().isCreated());
        return userId;
    }

    private InternalUserCreateRequest buildInternalUser(UUID userId, String email) {
        return InternalUserCreateRequest.builder()
                .id(userId)
                .name("John")
                .surname("Doe")
                .email(email)
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();
    }
}
