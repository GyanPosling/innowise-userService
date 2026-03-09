package com.innowise.userservice.controller;

import com.innowise.userservice.controller.api.PaymentCardControllerApi;
import com.innowise.userservice.model.dto.PaymentCardDto;
import com.innowise.userservice.security.SecurityUtil;
import com.innowise.userservice.service.PaymentCardService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
public class PaymentCardController implements PaymentCardControllerApi {

    private final PaymentCardService paymentCardService;
    private final SecurityUtil securityUtil;

    @PostMapping("/users/{userId}/cards")
    @PreAuthorize("hasRole('ADMIN') or @securityUtil.getAuthenticatedUserId() == #userId")
    @Override
    public ResponseEntity<PaymentCardDto> createCard(
            @PathVariable UUID userId,
            @Valid @RequestBody PaymentCardDto paymentCardDTO) {
        PaymentCardDto createdCard = paymentCardService.createCard(userId, paymentCardDTO);
        return new ResponseEntity<>(createdCard, HttpStatus.CREATED);
    }

    @GetMapping("/cards/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityUtil.isCardOwner(#id)")
    @Override
    public ResponseEntity<PaymentCardDto> getCard(
            @PathVariable Integer id) {
        PaymentCardDto card = paymentCardService.getCardById(id);
        return ResponseEntity.ok(card);
    }

    @GetMapping("/cards")
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<Page<PaymentCardDto>> getAllCards(
            @RequestParam(required = false) String holder,
            Pageable pageable) {
        Page<PaymentCardDto> cards = paymentCardService.getAllCards(holder, pageable);
        return ResponseEntity.ok(cards);
    }

    @GetMapping("/users/{userId}/cards")
    @PreAuthorize("hasRole('ADMIN') or @securityUtil.getAuthenticatedUserId() == #userId")
    @Override
    public ResponseEntity<List<PaymentCardDto>> getCardsByUser(
            @PathVariable UUID userId) {
        List<PaymentCardDto> cards = paymentCardService.getCardsByUserId(userId);
        return ResponseEntity.ok(cards);
    }

    @PutMapping("/cards/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<PaymentCardDto> updateCard(
            @PathVariable Integer id,
            @Valid @RequestBody PaymentCardDto paymentCardDTO) {
        PaymentCardDto updatedCard = paymentCardService.updateCard(id, paymentCardDTO);
        return ResponseEntity.ok(updatedCard);
    }

    @PatchMapping("/cards/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<PaymentCardDto> toggleCardStatus(
            @PathVariable Integer id) {
        PaymentCardDto card = paymentCardService.toggleCardStatus(id);
        return ResponseEntity.ok(card);
    }

    @DeleteMapping("/cards/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<Void> deleteCard(
            @PathVariable Integer id) {
        paymentCardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }
}
