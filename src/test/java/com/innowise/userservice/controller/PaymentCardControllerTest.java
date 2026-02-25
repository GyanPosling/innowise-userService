package com.innowise.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.userservice.AbstractIntegrationTest;
import com.innowise.userservice.model.dto.PaymentCardDto;
import com.innowise.userservice.model.dto.UserDto;
import com.innowise.userservice.repository.PaymentCardRepository;
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
class PaymentCardControllerTest extends AbstractIntegrationTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final PaymentCardRepository paymentCardRepository;
    private Integer userId;

    @BeforeEach
    void setUp() {
        paymentCardRepository.deleteAll();
        userRepository.deleteAll();

        UserDto userDto = UserDto.builder()
                .name("John")
                .surname("Doe")
                .email("john.doe@example.com")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

        try {
            String response = mockMvc.perform(post("/api/users")
                            .header(AUTH_HEADER, adminAuthHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userDto)))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            UserDto createdUser = objectMapper.readValue(response, UserDto.class);
            userId = createdUser.getId();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test user", e);
        }
    }

    @Test
    void createCard_ValidInput_ReturnsCreated() throws Exception {
        PaymentCardDto cardDto = PaymentCardDto.builder()
                .userId(userId)
                .number("1234567890123456")
                .holder("John Doe")
                .expirationDate(LocalDate.now().plusYears(2))
                .build();

        mockMvc.perform(post("/api/users/{userId}/cards", userId)
                        .header(AUTH_HEADER, adminAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.number", is("1234567890123456")))
                .andExpect(jsonPath("$.holder", is("John Doe")))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    void createCard_DuplicateCardNumber_ReturnsConflict() throws Exception {
        PaymentCardDto cardDto = PaymentCardDto.builder()
                .userId(userId)
                .number("1234567890123456")
                .holder("John Doe")
                .expirationDate(LocalDate.now().plusYears(2))
                .build();

        mockMvc.perform(post("/api/users/{userId}/cards", userId)
                        .header(AUTH_HEADER, adminAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardDto)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/users/{userId}/cards", userId)
                        .header(AUTH_HEADER, adminAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardDto)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void createCard_InvalidInput_ReturnsBadRequest() throws Exception {
        PaymentCardDto cardDto = PaymentCardDto.builder()
                .userId(userId)
                .number("123")
                .holder("")
                .expirationDate(LocalDate.now().minusDays(1))
                .build();

        mockMvc.perform(post("/api/users/{userId}/cards", userId)
                        .header(AUTH_HEADER, adminAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("number")))
                .andExpect(jsonPath("$.message", containsString("holder")))
                .andExpect(jsonPath("$.message", containsString("expirationDate")));
    }

    @Test
    void getCardById_CardExists_ReturnsCard() throws Exception {
        PaymentCardDto createDto = PaymentCardDto.builder()
                .userId(userId)
                .number("1234567890123456")
                .holder("John Doe")
                .expirationDate(LocalDate.now().plusYears(2))
                .build();

        String response = mockMvc.perform(post("/api/users/{userId}/cards", userId)
                        .header(AUTH_HEADER, adminAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        PaymentCardDto createdCard = objectMapper.readValue(response, PaymentCardDto.class);

        mockMvc.perform(get("/api/cards/{id}", createdCard.getId())
                        .header(AUTH_HEADER, adminAuthHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(createdCard.getId())))
                .andExpect(jsonPath("$.number", is("1234567890123456")));
    }

    @Test
    void getCardsByUserId_UserHasCards_ReturnsCards() throws Exception {
        for (int i = 1; i <= 3; i++) {
            PaymentCardDto cardDto = PaymentCardDto.builder()
                    .userId(userId)
                    .number("123456789012345" + i)
                    .holder("John Doe " + i)
                    .expirationDate(LocalDate.now().plusYears(2))
                    .build();

            mockMvc.perform(post("/api/users/{userId}/cards", userId)
                            .header(AUTH_HEADER, adminAuthHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cardDto)))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/users/{userId}/cards", userId)
                        .header(AUTH_HEADER, adminAuthHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void updateCard_ValidInput_ReturnsUpdatedCard() throws Exception {
        PaymentCardDto createDto = PaymentCardDto.builder()
                .userId(userId)
                .number("1234567890123456")
                .holder("John Doe")
                .expirationDate(LocalDate.now().plusYears(2))
                .build();

        String response = mockMvc.perform(post("/api/users/{userId}/cards", userId)
                        .header(AUTH_HEADER, adminAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        PaymentCardDto createdCard = objectMapper.readValue(response, PaymentCardDto.class);

        PaymentCardDto updateDto = PaymentCardDto.builder()
                .userId(userId)
                .number("9876543210987654")
                .holder("Jane Smith")
                .expirationDate(LocalDate.now().plusYears(3))
                .build();

        mockMvc.perform(put("/api/cards/{id}", createdCard.getId())
                        .header(AUTH_HEADER, adminAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number", is("9876543210987654")))
                .andExpect(jsonPath("$.holder", is("Jane Smith")));
    }

    @Test
    void updateCard_InvalidInput_ReturnsBadRequest() throws Exception {
        PaymentCardDto updateDto = PaymentCardDto.builder()
                .userId(userId)
                .number("bad")
                .holder(" ")
                .expirationDate(LocalDate.now().minusDays(5))
                .build();

        mockMvc.perform(put("/api/cards/{id}", 1)
                        .header(AUTH_HEADER, adminAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("number")))
                .andExpect(jsonPath("$.message", containsString("holder")))
                .andExpect(jsonPath("$.message", containsString("expirationDate")));
    }

    @Test
    void toggleCardStatus_CardExists_TogglesStatus() throws Exception {
        PaymentCardDto createDto = PaymentCardDto.builder()
                .userId(userId)
                .number("1234567890123456")
                .holder("John Doe")
                .expirationDate(LocalDate.now().plusYears(2))
                .build();

        String response = mockMvc.perform(post("/api/users/{userId}/cards", userId)
                        .header(AUTH_HEADER, adminAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        PaymentCardDto createdCard = objectMapper.readValue(response, PaymentCardDto.class);
        boolean initialStatus = createdCard.isActive();

        mockMvc.perform(patch("/api/cards/{id}", createdCard.getId())
                        .header(AUTH_HEADER, adminAuthHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(!initialStatus)));
    }

    @Test
    void getAllCards_WithFilter_ReturnsFilteredCards() throws Exception {
        for (int i = 1; i <= 3; i++) {
            PaymentCardDto cardDto = PaymentCardDto.builder()
                    .userId(userId)
                    .number("123456789012345" + i)
                    .holder("Holder " + i)
                    .expirationDate(LocalDate.now().plusYears(2))
                    .build();

            mockMvc.perform(post("/api/users/{userId}/cards", userId)
                            .header(AUTH_HEADER, adminAuthHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cardDto)))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/cards")
                        .header(AUTH_HEADER, adminAuthHeader())
                        .param("holder", "Holder 1")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].holder", containsString("Holder 1")));
    }

    @Test
    void deleteCard_CardExists_ReturnsDeletedCard() throws Exception {
        PaymentCardDto createDto = PaymentCardDto.builder()
                .userId(userId)
                .number("1234567890123456")
                .holder("John Doe")
                .expirationDate(LocalDate.now().plusYears(2))
                .build();

        String response = mockMvc.perform(post("/api/users/{userId}/cards", userId)
                        .header(AUTH_HEADER, adminAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        PaymentCardDto createdCard = objectMapper.readValue(response, PaymentCardDto.class);

        mockMvc.perform(delete("/api/cards/{id}", createdCard.getId())
                        .header(AUTH_HEADER, adminAuthHeader()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/cards/{id}", createdCard.getId())
                        .header(AUTH_HEADER, adminAuthHeader()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCard_CardNotFound_ReturnsNotFound() throws Exception {
        mockMvc.perform(delete("/api/cards/{id}", 999)
                        .header(AUTH_HEADER, adminAuthHeader()))
                .andExpect(status().isNotFound());
    }
}
