package com.innowise.userservice.service.impl;

import com.innowise.userservice.exception.LimitExceededException;
import com.innowise.userservice.exception.ResourceNotFoundException;
import com.innowise.userservice.exception.ValidationException;
import com.innowise.userservice.mapper.PaymentCardMapper;
import com.innowise.userservice.mapper.UserMapper;
import com.innowise.userservice.model.dto.PaymentCardDto;
import com.innowise.userservice.model.dto.UserDto;
import com.innowise.userservice.model.entity.PaymentCard;
import com.innowise.userservice.model.entity.User;
import com.innowise.userservice.repository.PaymentCardRepository;
import com.innowise.userservice.repository.specification.PaymentCardSpecification;
import com.innowise.userservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCardServiceImplTest {

    @Mock
    private PaymentCardRepository paymentCardRepository;

    @Mock
    private UserService userService;

    @Mock
    private PaymentCardSpecification paymentCardSpecification;

    @Mock
    private PaymentCardMapper paymentCardMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private PaymentCardServiceImpl paymentCardService;

    private PaymentCard card;
    private PaymentCardDto cardDto;
    private User user;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1)
                .name("John")
                .surname("Doe")
                .email("john.doe@example.com")
                .build();

        userDto = UserDto.builder()
                .id(1)
                .name("John")
                .surname("Doe")
                .email("john.doe@example.com")
                .build();

        card = PaymentCard.builder()
                .id(1)
                .user(user)
                .number("1234567890123456")
                .holder("John Doe")
                .expirationDate(LocalDate.now().plusYears(2))
                .active(true)
                .build();

        cardDto = PaymentCardDto.builder()
                .id(1)
                .userId(1)
                .number("1234567890123456")
                .holder("John Doe")
                .expirationDate(LocalDate.now().plusYears(2))
                .active(true)
                .build();
    }

    @Test
    void createCard_Success() {
        when(userService.getActiveCardCount(1)).thenReturn(3);
        when(paymentCardRepository.existsByUserIdAndCardNumber(1, cardDto.getNumber())).thenReturn(false);
        when(userService.getUserById(1)).thenReturn(userDto);
        when(userMapper.toEntity(userDto)).thenReturn(user);
        when(paymentCardMapper.toEntity(cardDto)).thenReturn(card);
        when(paymentCardRepository.save(card)).thenReturn(card);
        when(paymentCardMapper.toDTO(card)).thenReturn(cardDto);

        PaymentCardDto result = paymentCardService.createCard(1, cardDto);

        assertNotNull(result);
        assertEquals(cardDto.getNumber(), result.getNumber());
        verify(paymentCardRepository).save(card);
    }

    @Test
    void createCard_CardLimitExceeded_ThrowsException() {
        when(userService.getActiveCardCount(1)).thenReturn(5);

        LimitExceededException exception = assertThrows(LimitExceededException.class,
                () -> paymentCardService.createCard(1, cardDto));
        assertEquals("User cannot have more than 5 active cards", exception.getMessage());
    }

    @Test
    void createCard_DuplicateCardNumber_ThrowsException() {
        when(userService.getActiveCardCount(1)).thenReturn(3);
        when(paymentCardRepository.existsByUserIdAndCardNumber(1, cardDto.getNumber())).thenReturn(true);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> paymentCardService.createCard(1, cardDto));
        assertEquals("Card with this number already exists for this user", exception.getMessage());
    }

    @Test
    void getCardById_Success() {
        when(paymentCardRepository.findById(1)).thenReturn(Optional.of(card));
        when(paymentCardMapper.toDTO(card)).thenReturn(cardDto);

        PaymentCardDto result = paymentCardService.getCardById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        verify(paymentCardRepository).findById(1);
    }

    @Test
    void getCardById_NotFound_ThrowsException() {
        when(paymentCardRepository.findById(1)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> paymentCardService.getCardById(1));
        assertEquals("Card not found with id: 1", exception.getMessage());
    }

    @Test
    void getAllCards_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<PaymentCard> cardPage = new PageImpl<>(Collections.singletonList(card));

        when(paymentCardSpecification.hasHolder(any())).thenReturn((root, query, cb) -> null);
        when(paymentCardRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(cardPage);
        when(paymentCardMapper.toDTO(card)).thenReturn(cardDto);

        Page<PaymentCardDto> result = paymentCardService.getAllCards("John", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getCardsByUserId_Success() {
        List<PaymentCard> cards = Collections.singletonList(card);
        when(paymentCardRepository.findByUserId(1)).thenReturn(cards);
        when(paymentCardMapper.toDTO(card)).thenReturn(cardDto);

        List<PaymentCardDto> result = paymentCardService.getCardsByUserId(1);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(paymentCardRepository).findByUserId(1);
    }

    @Test
    void updateCard_Success() {
        PaymentCardDto updateDto = PaymentCardDto.builder()
                .number("9876543210987654")
                .holder("Jane Smith")
                .expirationDate(LocalDate.now().plusYears(3))
                .build();

        when(paymentCardRepository.findById(1)).thenReturn(Optional.of(card));
        when(paymentCardRepository.save(card)).thenReturn(card);
        when(paymentCardMapper.toDTO(card)).thenReturn(cardDto);

        PaymentCardDto result = paymentCardService.updateCard(1, updateDto);

        assertNotNull(result);
        verify(paymentCardRepository).save(card);
        assertEquals("9876543210987654", card.getNumber());
        assertEquals("Jane Smith", card.getHolder());
    }

    @Test
    void updateCard_NotFound_ThrowsException() {
        when(paymentCardRepository.findById(1)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> paymentCardService.updateCard(1, cardDto));
        assertEquals("Card not found with id: 1", exception.getMessage());
    }

    @Test
    void toggleCardStatus_Success() {
        boolean initialStatus = card.isActive();

        when(paymentCardRepository.findById(1)).thenReturn(Optional.of(card));
        when(paymentCardRepository.save(card)).thenReturn(card);
        when(paymentCardMapper.toDTO(card)).thenReturn(cardDto);

        PaymentCardDto result = paymentCardService.toggleCardStatus(1);

        assertNotNull(result);
        assertEquals(!initialStatus, card.isActive());
        verify(paymentCardRepository).save(card);
    }

    @Test
    void toggleCardStatus_NotFound_ThrowsException() {
        when(paymentCardRepository.findById(1)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> paymentCardService.toggleCardStatus(1));
        assertTrue(exception.getMessage().contains("Card not found"));
    }

    @Test
    void deleteCard_Success() {
        when(paymentCardRepository.findUserIdByCardId(1)).thenReturn(Optional.of(1));

        paymentCardService.deleteCard(1);

        verify(paymentCardRepository).deleteById(1);
    }

    @Test
    void deleteCard_NotFound_ThrowsException() {
        when(paymentCardRepository.findUserIdByCardId(1)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> paymentCardService.deleteCard(1));
        assertEquals("Card not found with id: 1", exception.getMessage());
        verify(paymentCardRepository, never()).deleteById(anyInt());
    }
}
