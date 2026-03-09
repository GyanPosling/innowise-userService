package com.innowise.userservice.controller.api;

import com.innowise.userservice.model.dto.PaymentCardDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

@Tag(name = "Payment Cards", description = "Payment card management endpoints")
@SecurityRequirement(name = "bearerAuth")
public interface PaymentCardControllerApi {

    @Operation(summary = "Create payment card for user")
    @ApiResponse(responseCode = "201", description = "Card created",
            content = @Content(schema = @Schema(implementation = PaymentCardDto.class)))
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "409", description = "Card already exists")
    ResponseEntity<PaymentCardDto> createCard(
            @Parameter(description = "User id") UUID userId,
            @Valid
            @RequestBody(required = true, description = "Card payload",
                    content = @Content(schema = @Schema(implementation = PaymentCardDto.class)))
            PaymentCardDto paymentCardDTO);

    @Operation(summary = "Get payment card by id")
    @ApiResponse(responseCode = "200", description = "Card found",
            content = @Content(schema = @Schema(implementation = PaymentCardDto.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Card not found")
    ResponseEntity<PaymentCardDto> getCard(
            @Parameter(description = "Card id") Integer id);

    @Operation(summary = "Get paged payment card list")
    @ApiResponse(responseCode = "200", description = "Page returned")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    ResponseEntity<Page<PaymentCardDto>> getAllCards(
            @Parameter(description = "Holder filter") String holder,
            Pageable pageable);

    @Operation(summary = "Get all cards for user")
    @ApiResponse(responseCode = "200", description = "Cards found",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = PaymentCardDto.class))))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    ResponseEntity<List<PaymentCardDto>> getCardsByUser(
            @Parameter(description = "User id") UUID userId);

    @Operation(summary = "Update payment card")
    @ApiResponse(responseCode = "200", description = "Card updated",
            content = @Content(schema = @Schema(implementation = PaymentCardDto.class)))
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Card not found")
    ResponseEntity<PaymentCardDto> updateCard(
            @Parameter(description = "Card id") Integer id,
            @Valid
            @RequestBody(required = true, description = "Updated card payload",
                    content = @Content(schema = @Schema(implementation = PaymentCardDto.class)))
            PaymentCardDto paymentCardDTO);

    @Operation(summary = "Toggle payment card active status")
    @ApiResponse(responseCode = "200", description = "Card status toggled",
            content = @Content(schema = @Schema(implementation = PaymentCardDto.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Card not found")
    ResponseEntity<PaymentCardDto> toggleCardStatus(
            @Parameter(description = "Card id") Integer id);

    @Operation(summary = "Delete payment card")
    @ApiResponse(responseCode = "204", description = "Card deleted")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Card not found")
    ResponseEntity<Void> deleteCard(
            @Parameter(description = "Card id") Integer id);
}
