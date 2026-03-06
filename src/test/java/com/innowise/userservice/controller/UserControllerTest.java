package com.innowise.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.userservice.AbstractIntegrationTest;
import com.innowise.userservice.model.dto.InternalUserCreateRequest;
import com.innowise.userservice.model.dto.InternalUserCreateResponse;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
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
    void createUser_ValidInput_ReturnsCreated() throws Exception {
        InternalUserCreateRequest request = InternalUserCreateRequest.builder()
                .name("John")
                .surname("Doe")
                .email("john.doe@example.com")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

        String response = mockMvc.perform(post("/api/internal/users")
                        .headers(internalHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        InternalUserCreateResponse createdUser = objectMapper.readValue(response, InternalUserCreateResponse.class);

        mockMvc.perform(get("/api/users/{id}", createdUser.getUserId())
                        .headers(adminHeaders("GET", "/api/users/" + createdUser.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(createdUser.getUserId())))
                .andExpect(jsonPath("$.name", is("John")))
                .andExpect(jsonPath("$.email", is("john.doe@example.com")))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    void createUser_DuplicateEmail_ReturnsConflict() throws Exception {
        InternalUserCreateRequest request = InternalUserCreateRequest.builder()
                .name("John")
                .surname("Doe")
                .email("john.doe@example.com")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

        mockMvc.perform(post("/api/internal/users")
                        .headers(internalHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/internal/users")
                        .headers(internalHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void createUser_InvalidInput_ReturnsBadRequest() throws Exception {
        InternalUserCreateRequest request = InternalUserCreateRequest.builder()
                .name("")
                .surname("D")
                .email("not-an-email")
                .birthDate(LocalDate.now().plusDays(1))
                .build();

        mockMvc.perform(post("/api/internal/users")
                        .headers(internalHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("name")))
                .andExpect(jsonPath("$.message", containsString("surname")))
                .andExpect(jsonPath("$.message", containsString("email")))
                .andExpect(jsonPath("$.message", containsString("birthDate")));
    }

    @Test
    void getUserById_UserExists_ReturnsUser() throws Exception {
        InternalUserCreateRequest request = InternalUserCreateRequest.builder()
                .name("John")
                .surname("Doe")
                .email("john.doe@example.com")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

        String response = mockMvc.perform(post("/api/internal/users")
                        .headers(internalHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        InternalUserCreateResponse createdUser = objectMapper.readValue(response, InternalUserCreateResponse.class);

        mockMvc.perform(get("/api/users/{id}", createdUser.getUserId())
                        .headers(adminHeaders("GET", "/api/users/" + createdUser.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(createdUser.getUserId())))
                .andExpect(jsonPath("$.name", is("John")));
    }

    @Test
    void getUserById_UserNotExists_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/users/{id}", 999)
                        .headers(adminHeaders("GET", "/api/users/999")))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllUsers_WithPagination_ReturnsPage() throws Exception {
        for (int i = 1; i <= 3; i++) {
            InternalUserCreateRequest request = InternalUserCreateRequest.builder()
                    .name("User" + i)
                    .surname("Test")
                    .email("user" + i + "@example.com")
                    .birthDate(LocalDate.of(1990, 1, i))
                    .build();

            mockMvc.perform(post("/api/internal/users")
                            .headers(internalHeaders())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

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
        InternalUserCreateRequest createDto = InternalUserCreateRequest.builder()
                .name("John")
                .surname("Doe")
                .email("john.doe@example.com")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

        String response = mockMvc.perform(post("/api/internal/users")
                        .headers(internalHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        InternalUserCreateResponse createdUser = objectMapper.readValue(response, InternalUserCreateResponse.class);

        UserDto updateDto = UserDto.builder()
                .name("Jane")
                .surname("Smith")
                .email("jane.smith@example.com")
                .birthDate(LocalDate.of(1992, 2, 2))
                .build();

        mockMvc.perform(put("/api/users/{id}", createdUser.getUserId())
                        .headers(adminHeaders("PUT", "/api/users/" + createdUser.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Jane")))
                .andExpect(jsonPath("$.surname", is("Smith")));
    }

    @Test
    void updateUser_InvalidInput_ReturnsBadRequest() throws Exception {
        UserDto updateDto = UserDto.builder()
                .name(" ")
                .surname("")
                .email("bad")
                .birthDate(LocalDate.now().plusDays(1))
                .build();

        mockMvc.perform(put("/api/users/{id}", 1)
                        .headers(adminHeaders("PUT", "/api/users/1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("name")))
                .andExpect(jsonPath("$.message", containsString("surname")))
                .andExpect(jsonPath("$.message", containsString("email")))
                .andExpect(jsonPath("$.message", containsString("birthDate")));
    }

    @Test
    void toggleUserStatus_UserExists_TogglesStatus() throws Exception {
        InternalUserCreateRequest createDto = InternalUserCreateRequest.builder()
                .name("John")
                .surname("Doe")
                .email("john.doe@example.com")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

        String response = mockMvc.perform(post("/api/internal/users")
                        .headers(internalHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        InternalUserCreateResponse createdUser = objectMapper.readValue(response, InternalUserCreateResponse.class);

        String createdUserResponse = mockMvc.perform(get("/api/users/{id}", createdUser.getUserId())
                        .headers(adminHeaders("GET", "/api/users/" + createdUser.getUserId())))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UserDto createdUserDetails = objectMapper.readValue(createdUserResponse, UserDto.class);
        boolean initialStatus = createdUserDetails.isActive();

        mockMvc.perform(patch("/api/users/{id}", createdUser.getUserId())
                        .headers(adminHeaders("PATCH", "/api/users/" + createdUser.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(!initialStatus)));
    }

    @Test
    void deleteUser_UserExists_ReturnsDeletedUser() throws Exception {
        InternalUserCreateRequest createDto = InternalUserCreateRequest.builder()
                .name("John")
                .surname("Doe")
                .email("john.doe@example.com")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

        String response = mockMvc.perform(post("/api/internal/users")
                        .headers(internalHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        InternalUserCreateResponse createdUser = objectMapper.readValue(response, InternalUserCreateResponse.class);

        mockMvc.perform(delete("/api/users/{id}", createdUser.getUserId())
                        .headers(adminHeaders("DELETE", "/api/users/" + createdUser.getUserId())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/{id}", createdUser.getUserId())
                        .headers(adminHeaders("GET", "/api/users/" + createdUser.getUserId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_UserNotFound_ReturnsNotFound() throws Exception {
        mockMvc.perform(delete("/api/users/{id}", 999)
                        .headers(adminHeaders("DELETE", "/api/users/999")))
                .andExpect(status().isNotFound());
    }
}
