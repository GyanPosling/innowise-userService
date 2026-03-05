package com.innowise.userservice.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalUserAuthLinkRequest {
    @NotNull(message = "Auth user id is required")
    private Long authUserId;
}
