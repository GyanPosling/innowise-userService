package com.innowise.userservice.repository;

import com.innowise.userservice.model.entity.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    List<User> findAllByEmailIn(Collection<String> emails);

    @Modifying
    @Query("UPDATE User u SET u.active = :status WHERE u.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("status") boolean status);

    @Query(value = "SELECT COUNT(*) FROM payment_cards WHERE user_id = :userId AND active = true",
            nativeQuery = true)
    int getActiveCardCount(@Param("userId") UUID userId);
}
