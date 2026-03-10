package com.innowise.userservice.service.impl;

import com.innowise.userservice.exception.ResourceNotFoundException;
import com.innowise.userservice.exception.ValidationException;
import com.innowise.userservice.mapper.UserMapper;
import com.innowise.userservice.model.dto.UserDto;
import com.innowise.userservice.model.entity.User;
import com.innowise.userservice.repository.UserRepository;
import com.innowise.userservice.repository.specification.UserSpecification;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSpecification userSpecification;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(USER_ID)
                .name("John")
                .surname("Doe")
                .email("john.doe@example.com")
                .birthDate(LocalDate.of(1990, 1, 1))
                .active(true)
                .build();

        userDto = UserDto.builder()
                .id(USER_ID)
                .name("John")
                .surname("Doe")
                .email("john.doe@example.com")
                .birthDate(LocalDate.of(1990, 1, 1))
                .active(true)
                .build();
    }

    @Test
    void createUser_Success() {
        when(userRepository.existsById(USER_ID)).thenReturn(false);
        when(userRepository.findByEmail(userDto.getEmail())).thenReturn(Optional.empty());
        when(userMapper.toEntity(userDto)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(userDto);

        UserDto result = userService.createUser(userDto);

        assertNotNull(result);
        assertEquals(userDto.getEmail(), result.getEmail());
        assertTrue(user.isActive());
        verify(userRepository).save(user);
    }

    @Test
    void createUser_WhenIdMissing_ThrowsException() {
        userDto.setId(null);

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> userService.createUser(userDto)
        );

        assertEquals("User id is required", exception.getMessage());
    }

    @Test
    void createUser_WhenIdAlreadyExists_ThrowsException() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> userService.createUser(userDto)
        );

        assertEquals("User with this id already exists", exception.getMessage());
    }

    @Test
    void createUser_EmailAlreadyExists_ThrowsException() {
        when(userRepository.existsById(USER_ID)).thenReturn(false);
        when(userRepository.findByEmail(userDto.getEmail())).thenReturn(Optional.of(user));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> userService.createUser(userDto)
        );

        assertEquals("User with this email already exists", exception.getMessage());
    }

    @Test
    void getUserById_Success() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userMapper.toDTO(user)).thenReturn(userDto);

        UserDto result = userService.getUserById(USER_ID);

        assertNotNull(result);
        assertEquals(USER_ID, result.getId());
    }

    @Test
    void getUserById_NotFound_ThrowsException() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getUserById(USER_ID)
        );

        assertEquals("User not found with id: " + USER_ID, exception.getMessage());
    }

    @Test
    void getAllUsers_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(Collections.singletonList(user));

        when(userSpecification.hasFirstName(any())).thenReturn((root, query, cb) -> null);
        when(userSpecification.hasLastName(any())).thenReturn((root, query, cb) -> null);
        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(userPage);
        when(userMapper.toDTO(user)).thenReturn(userDto);

        Page<UserDto> result = userService.getAllUsers("John", "Doe", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void updateUser_Success() {
        UserDto updateDto = UserDto.builder()
                .name("Jane")
                .surname("Smith")
                .email("jane.smith@example.com")
                .birthDate(LocalDate.of(1992, 2, 2))
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail(updateDto.getEmail())).thenReturn(Optional.empty());
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(userDto);

        UserDto result = userService.updateUser(USER_ID, updateDto);

        assertNotNull(result);
        assertEquals("Jane", user.getName());
        assertEquals("Smith", user.getSurname());
        verify(userRepository).save(user);
    }

    @Test
    void updateUser_EmailAlreadyExists_ThrowsException() {
        UserDto updateDto = UserDto.builder()
                .name("Jane")
                .surname("Smith")
                .email("jane.smith@example.com")
                .birthDate(LocalDate.of(1992, 2, 2))
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail(updateDto.getEmail())).thenReturn(Optional.of(new User()));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> userService.updateUser(USER_ID, updateDto)
        );

        assertEquals("User with this email already exists", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void toggleUserStatus_Success() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(userDto);

        userService.toggleUserStatus(USER_ID);

        assertEquals(false, user.isActive());
        verify(userRepository).save(user);
    }

    @Test
    void getActiveCardCount_Success() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(userRepository.getActiveCardCount(USER_ID)).thenReturn(3);

        int count = userService.getActiveCardCount(USER_ID);

        assertEquals(3, count);
    }

    @Test
    void deleteUser_Success() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        userService.deleteUser(USER_ID);

        verify(userRepository).deleteById(USER_ID);
    }
}
