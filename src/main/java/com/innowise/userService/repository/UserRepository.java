package com.innowise.userService.repository;

import com.innowise.userService.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    List<User> findAllByEmailIn(Collection<String> emails);

    @Modifying
    @Query("UPDATE User u SET u.active = :status WHERE u.id = :id")
    void updateStatus(@Param("id") Integer id, @Param("status") boolean status);

    @Query(value = "SELECT COUNT(*) FROM payment_cards WHERE user_id = :userId AND active = true",
            nativeQuery = true)
    int getActiveCardCount(@Param("userId") Integer userId);
}
