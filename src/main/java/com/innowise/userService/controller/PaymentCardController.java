package com.innowise.userservice.controller;

import com.innowise.userservice.model.dto.PaymentCardDto;
import com.innowise.userservice.security.SecurityUtil;
import com.innowise.userservice.service.PaymentCardService;
import jakarta.validation.Valid;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
@Tag(name = "Payment Cards", description = "Payment card management API")
public class PaymentCardController {

    private final PaymentCardService paymentCardService;
    private final SecurityUtil securityUtil;

    @Operation(summary = "Create a new payment card for user (max 5 cards per user)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Card created"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "409", description = "Card limit exceeded or duplicate card")
    })
    @PostMapping("/users/{userId}/cards")
    @PreAuthorize("hasRole('ADMIN') or @securityUtil.getAuthenticatedUserId() == #userId")
    public ResponseEntity<PaymentCardDto> createCard(
            @Parameter(description = "ID of the user", required = true, example = "1")
            @PathVariable Integer userId,

            @Valid @RequestBody PaymentCardDto paymentCardDTO) {
        PaymentCardDto createdCard = paymentCardService.createCard(userId, paymentCardDTO);
        return new ResponseEntity<>(createdCard, HttpStatus.CREATED);
    }

    @Operation(summary = "Get payment card by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Card found"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @GetMapping("/cards/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityUtil.isCardOwner(#id)")
    public ResponseEntity<PaymentCardDto> getCard(
            @Parameter(description = "ID of the card", required = true, example = "1")
            @PathVariable Integer id) {
        PaymentCardDto card = paymentCardService.getCardById(id);
        return ResponseEntity.ok(card);
    }

    @Operation(summary = "Get all cards with filter and pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cards retrieved")
    })
    @GetMapping("/cards")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<PaymentCardDto>> getAllCards(
            @Parameter(description = "Filter by card holder name")
            @RequestParam(required = false) String holder,

            @Parameter(description = "Pagination parameters")
            Pageable pageable) {
        Page<PaymentCardDto> cards = paymentCardService.getAllCards(holder, pageable);
        return ResponseEntity.ok(cards);
    }

    @Operation(summary = "Get all cards by user ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cards retrieved"),
            @ApiResponse(responseCode = "404", description = "User not found or has no cards")
    })
    @GetMapping("/users/{userId}/cards")
    @PreAuthorize("hasRole('ADMIN') or @securityUtil.getAuthenticatedUserId() == #userId")
    public ResponseEntity<List<PaymentCardDto>> getCardsByUser(
            @Parameter(description = "ID of the user", required = true, example = "1")
            @PathVariable Integer userId) {
        List<PaymentCardDto> cards = paymentCardService.getCardsByUserId(userId);
        return ResponseEntity.ok(cards);
    }

    @Operation(summary = "Update card by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Card updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @PutMapping("/cards/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentCardDto> updateCard(
            @Parameter(description = "ID of the card", required = true, example = "1")
            @PathVariable Integer id,

            @Valid @RequestBody PaymentCardDto paymentCardDTO) {
        PaymentCardDto updatedCard = paymentCardService.updateCard(id, paymentCardDTO);
        return ResponseEntity.ok(updatedCard);
    }

    @Operation(summary = "Toggle card status (active/inactive)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status toggled"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @PatchMapping("/cards/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentCardDto> toggleCardStatus(
            @Parameter(description = "ID of the card", required = true, example = "1")
            @PathVariable Integer id) {
        PaymentCardDto card = paymentCardService.toggleCardStatus(id);
        return ResponseEntity.ok(card);
    }

    @Operation(summary = "Delete card by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Card deleted"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @DeleteMapping("/cards/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCard(
            @Parameter(description = "ID of the card", required = true, example = "1")
            @PathVariable Integer id) {
        paymentCardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }
}
