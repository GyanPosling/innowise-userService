package com.innowise.userservice.service;

import com.innowise.userservice.model.dto.UserDto;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service API for managing users.
 */
public interface UserService {

    /**
     * Creates a user.
     *
     * @param userDTO user data
     * @return created user
     */
    UserDto createUser(UserDto userDTO);

    /**
     * Retrieves a user by id.
     *
     * @param id user id
     * @return user
     */
    UserDto getUserById(UUID id);

    /**
     * Retrieves a user by email.
     *
     * @param email user email
     * @return user
     */
    UserDto getUserByEmail(String email);

    /**
     * Retrieves users by emails.
     *
     * @param emails user emails
     * @return list of users
     */
    List<UserDto> getUsersByEmails(Collection<String> emails);

    /**
     * Retrieves users with optional filters and pagination.
     *
     * @param firstName first name filter
     * @param lastName last name filter
     * @param pageable pagination info
     * @return page of users
     */
    Page<UserDto> getAllUsers(String firstName, String lastName, Pageable pageable);

    /**
     * Updates user data.
     *
     * @param id user id
     * @param userDTO user data
     * @return updated user
     */
    UserDto updateUser(UUID id, UserDto userDTO);

    /**
     * Toggles user active status.
     *
     * @param id user id
     * @return updated user
     */
    UserDto toggleUserStatus(UUID id);

    /**
     * Counts active cards for a user.
     *
     * @param userId user id
     * @return active card count
     */
    int getActiveCardCount(UUID userId);

    /**
     * Deletes a user by id.
     *
     * @param id user id
     */
    void deleteUser(UUID id);
}
