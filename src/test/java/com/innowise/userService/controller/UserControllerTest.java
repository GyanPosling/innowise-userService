package com.innowise.userService.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.userService.AbstractIntegrationTest;
import com.innowise.userService.model.dto.UserDto;
import com.innowise.userService.repository.UserRepository;
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
        UserDto userDto = UserDto.builder()
                .name("John")
                .surname("Doe")
                .email("john.doe@example.com")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

        mockMvc.perform(post("/api/users")
                        .header(AUTH_HEADER, userAuthHeader(1L, "auth1@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("John")))
                .andExpect(jsonPath("$.email", is("john.doe@example.com")))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    void createUser_DuplicateEmail_ReturnsConflict() throws Exception {
        UserDto userDto = UserDto.builder()
                .name("John")
                .surname("Doe")
                .email("john.doe@example.com")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

        mockMvc.perform(post("/api/users")
                        .header(AUTH_HEADER, userAuthHeader(1L, "auth1@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/users")
                        .header(AUTH_HEADER, userAuthHeader(2L, "auth2@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void createUser_InvalidInput_ReturnsBadRequest() throws Exception {
        UserDto userDto = UserDto.builder()
                .name("")
                .surname("D")
                .email("not-an-email")
                .birthDate(LocalDate.now().plusDays(1))
                .build();

        mockMvc.perform(post("/api/users")
                        .header(AUTH_HEADER, userAuthHeader(1L, "auth1@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("name")))
                .andExpect(jsonPath("$.message", containsString("surname")))
                .andExpect(jsonPath("$.message", containsString("email")))
                .andExpect(jsonPath("$.message", containsString("birthDate")));
    }

    @Test
    void getUserById_UserExists_ReturnsUser() throws Exception {
        UserDto userDto = UserDto.builder()
                .name("John")
                .surname("Doe")
                .email("john.doe@example.com")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

        String response = mockMvc.perform(post("/api/users")
                        .header(AUTH_HEADER, userAuthHeader(1L, "auth1@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UserDto createdUser = objectMapper.readValue(response, UserDto.class);

        mockMvc.perform(get("/api/users/{id}", createdUser.getId())
                        .header(AUTH_HEADER, adminAuthHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(createdUser.getId())))
                .andExpect(jsonPath("$.name", is("John")));
    }

    @Test
    void getUserById_UserNotExists_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/users/{id}", 999)
                        .header(AUTH_HEADER, adminAuthHeader()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllUsers_WithPagination_ReturnsPage() throws Exception {
        for (int i = 1; i <= 3; i++) {
            UserDto userDto = UserDto.builder()
                    .name("User" + i)
                    .surname("Test")
                    .email("user" + i + "@example.com")
                    .birthDate(LocalDate.of(1990, 1, i))
                    .build();

            mockMvc.perform(post("/api/users")
                            .header(AUTH_HEADER, userAuthHeader((long) i, "auth" + i + "@example.com"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userDto)))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/users")
                        .header(AUTH_HEADER, adminAuthHeader())
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", is(3)));
    }

    @Test
    void updateUser_ValidInput_ReturnsUpdatedUser() throws Exception {
        UserDto createDto = UserDto.builder()
                .name("John")
                .surname("Doe")
                .email("john.doe@example.com")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

        String response = mockMvc.perform(post("/api/users")
                        .header(AUTH_HEADER, userAuthHeader(1L, "auth1@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UserDto createdUser = objectMapper.readValue(response, UserDto.class);

        UserDto updateDto = UserDto.builder()
                .name("Jane")
                .surname("Smith")
                .email("jane.smith@example.com")
                .birthDate(LocalDate.of(1992, 2, 2))
                .build();

        mockMvc.perform(put("/api/users/{id}", createdUser.getId())
                        .header(AUTH_HEADER, adminAuthHeader())
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
                        .header(AUTH_HEADER, adminAuthHeader())
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
        UserDto createDto = UserDto.builder()
                .name("John")
                .surname("Doe")
                .email("john.doe@example.com")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

        String response = mockMvc.perform(post("/api/users")
                        .header(AUTH_HEADER, userAuthHeader(1L, "auth1@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UserDto createdUser = objectMapper.readValue(response, UserDto.class);
        boolean initialStatus = createdUser.isActive();

        mockMvc.perform(patch("/api/users/{id}", createdUser.getId())
                        .header(AUTH_HEADER, adminAuthHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(!initialStatus)));
    }

    @Test
    void deleteUser_UserExists_ReturnsDeletedUser() throws Exception {
        UserDto createDto = UserDto.builder()
                .name("John")
                .surname("Doe")
                .email("john.doe@example.com")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

        String response = mockMvc.perform(post("/api/users")
                        .header(AUTH_HEADER, userAuthHeader(1L, "auth1@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UserDto createdUser = objectMapper.readValue(response, UserDto.class);

        mockMvc.perform(delete("/api/users/{id}", createdUser.getId())
                        .header(AUTH_HEADER, adminAuthHeader()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/{id}", createdUser.getId())
                        .header(AUTH_HEADER, adminAuthHeader()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_UserNotFound_ReturnsNotFound() throws Exception {
        mockMvc.perform(delete("/api/users/{id}", 999)
                        .header(AUTH_HEADER, adminAuthHeader()))
                .andExpect(status().isNotFound());
    }
}
