package com.innowise.userService.repository;

import com.innowise.userService.model.entity.PaymentCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentCardRepository extends JpaRepository<PaymentCard, Integer>, JpaSpecificationExecutor<PaymentCard> {

    List<PaymentCard> findByUserId(Integer userId);

    Page<PaymentCard> findByUserId(Integer userId, Pageable pageable);

    @Modifying
    @Query("UPDATE PaymentCard pc SET pc.active = :active WHERE pc.id = :id")
    void updateStatus(@Param("id") Integer id, @Param("active") boolean active);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM payment_cards WHERE user_id = :userId AND number = :cardNumber)",
            nativeQuery = true)
    boolean existsByUserIdAndCardNumber(@Param("userId") Integer userId, @Param("cardNumber") String cardNumber);

    @Query("SELECT pc.user.id FROM PaymentCard pc WHERE pc.id = :id")
    Optional<Integer> findUserIdByCardId(@Param("id") Integer id);
}
