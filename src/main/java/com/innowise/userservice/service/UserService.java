package com.innowise.userservice.service;

import com.innowise.userservice.model.dto.InternalUserCreateRequest;
import com.innowise.userservice.model.dto.UserDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;

/**
 * Service API for managing users.
 */
public interface UserService {

    UserDto createInternalUser(InternalUserCreateRequest request);

    /**
     * Retrieves a user by id.
     *
     * @param id user id
     * @return user
     */
    UserDto getUserById(Integer id);

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
    UserDto updateUser(Integer id, UserDto userDTO);

    /**
     * Toggles user active status.
     *
     * @param id user id
     * @return updated user
     */
    UserDto toggleUserStatus(Integer id);

    /**
     * Counts active cards for a user.
     *
     * @param userId user id
     * @return active card count
     */
    int getActiveCardCount(Integer userId);

    /**
     * Deletes a user by id.
     *
     * @param id user id
     */
    void deleteUser(Integer id);

    void linkAuthUserId(Integer userId, Long authUserId);

    void deleteInternalUser(Integer userId);
}
