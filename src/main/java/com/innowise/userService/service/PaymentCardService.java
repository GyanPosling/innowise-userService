package com.innowise.userService.service;

import com.innowise.userService.model.dto.PaymentCardDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;


/**
 * Service API for managing payment cards.
 */
public interface PaymentCardService {

    /**
     * Creates a payment card for a user.
     *
     * @param userId user id
     * @param paymentCardDTO card data
     * @return created card
     */
    PaymentCardDto createCard(Integer userId, PaymentCardDto paymentCardDTO);

    /**
     * Retrieves a card by id.
     *
     * @param id card id
     * @return card
     */
    PaymentCardDto getCardById(Integer id);

    /**
     * Retrieves cards with optional filter and pagination.
     *
     * @param holder holder filter
     * @param pageable pagination info
     * @return page of cards
     */
    Page<PaymentCardDto> getAllCards(String holder, Pageable pageable);

    /**
     * Retrieves all cards for a user.
     *
     * @param userId user id
     * @return list of cards
     */
    List<PaymentCardDto> getCardsByUserId(Integer userId);

    /**
     * Updates card data.
     *
     * @param id card id
     * @param paymentCardDTO card data
     * @return updated card
     */
    PaymentCardDto updateCard(Integer id, PaymentCardDto paymentCardDTO);

    /**
     * Toggles card active status.
     *
     * @param id card id
     * @return updated card
     */
    PaymentCardDto toggleCardStatus(Integer id);

    /**
     * Deletes a card by id.
     *
     * @param id card id
     */
    void deleteCard(Integer id);
}
