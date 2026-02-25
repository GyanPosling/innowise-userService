package com.innowise.userService.service.impl;

import com.innowise.userService.exception.ResourceNotFoundException;
import com.innowise.userService.exception.ValidationException;
import com.innowise.userService.mapper.UserMapper;
import com.innowise.userService.model.dto.UserDto;
import com.innowise.userService.model.entity.User;
import com.innowise.userService.repository.UserRepository;
import com.innowise.userService.repository.specification.UserSpecification;
import com.innowise.userService.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserSpecification userSpecification;
    private final UserMapper userMapper;
    private final CacheManager cacheManager;

    @Override
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public UserDto createUser(UserDto userDTO) {
        if (userRepository.findByEmail(userDTO.getEmail()).isPresent()) {
            throw new ValidationException("User with this email already exists");
        }
        User user = userMapper.toEntity(userDTO);
        user.setActive(true);
        User savedUser = userRepository.save(user);
        return userMapper.toDTO(savedUser);
    }

    @Override
    @Cacheable(value = "users", key = "#id", unless = "#result == null")
    public UserDto getUserById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return userMapper.toDTO(user);
    }

    @Override
    @Cacheable(value = "usersByEmail", key = "#email", unless = "#result == null")
    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return userMapper.toDTO(user);
    }

    @Override
    @Cacheable(value = "usersByEmails", key = "#emails", unless = "#result == null || #result.isEmpty()")
    public List<UserDto> getUsersByEmails(Collection<String> emails) {
        if (emails == null || emails.isEmpty()) {
            return List.of();
        }
        List<User> users = userRepository.findAllByEmailIn(emails);
        return users.stream()
                .map(userMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "usersPage", key = "{#firstName, #lastName, #pageable.pageNumber, #pageable.pageSize, #pageable.sort}")
    public Page<UserDto> getAllUsers(String firstName, String lastName, Pageable pageable) {
        Specification<User> spec = Specification.where((Specification<User>) null);
        if (firstName != null && !firstName.isBlank()) {
            spec = spec.and(userSpecification.hasFirstName(firstName));
        }
        if (lastName != null && !lastName.isBlank()) {
            spec = spec.and(userSpecification.hasLastName(lastName));
        }
        Page<User> users = userRepository.findAll(spec, pageable);
        return users.map(userMapper::toDTO);
    }

    @Override
    @Transactional
    @Caching(
            put = @CachePut(value = "users", key = "#id"),
            evict = {
                    @CacheEvict(value = "usersPage", allEntries = true),
                    @CacheEvict(value = "userCards", key = "#id")
            }
    )
    public UserDto updateUser(Integer id, UserDto userDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        String newEmail = userDTO.getEmail();
        if (newEmail != null && !newEmail.equals(user.getEmail())
                && userRepository.findByEmail(newEmail).isPresent()) {
            throw new ValidationException("User with this email already exists");
        }
        user.setName(userDTO.getName());
        user.setSurname(userDTO.getSurname());
        user.setBirthDate(userDTO.getBirthDate());
        user.setEmail(newEmail);
        User updatedUser = userRepository.save(user);
        return userMapper.toDTO(updatedUser);
    }

    @Override
    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = "users", key = "#id"),
                    @CacheEvict(value = "usersPage", allEntries = true),
                    @CacheEvict(value = "userCards", key = "#id")
            }
    )
    public UserDto toggleUserStatus(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        boolean newStatus = !user.isActive();
        user.setActive(newStatus);
        User updatedUser = userRepository.save(user);
        return userMapper.toDTO(updatedUser);
    }

    @Override
    @Cacheable(value = "userCards", key = "#userId", unless = "#result == 0")
    public int getActiveCardCount(Integer userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        return userRepository.getActiveCardCount(userId);
    }

    @Override
    @Transactional
    public void deleteUser(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        userRepository.deleteById(id);
        evictUserCaches(id);
    }

    private void evictUserCaches(Integer userId) {
        Cache usersCache = cacheManager.getCache("users");
        if (usersCache != null) {
            usersCache.evict(userId);
        }
        Cache usersPageCache = cacheManager.getCache("usersPage");
        if (usersPageCache != null) {
            usersPageCache.clear();
        }
        Cache userCardsCache = cacheManager.getCache("userCards");
        if (userCardsCache != null) {
            userCardsCache.evict(userId);
        }
        Cache userCardsListCache = cacheManager.getCache("userCardsList");
        if (userCardsListCache != null) {
            userCardsListCache.evict(userId);
        }
        Cache cardsCache = cacheManager.getCache("cards");
        if (cardsCache != null) {
            cardsCache.clear();
        }
        Cache cardsPageCache = cacheManager.getCache("cardsPage");
        if (cardsPageCache != null) {
            cardsPageCache.clear();
        }
    }
}
