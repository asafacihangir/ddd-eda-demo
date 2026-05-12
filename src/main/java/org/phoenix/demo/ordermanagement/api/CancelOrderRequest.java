package org.phoenix.demo.ordermanagement.api;

import jakarta.validation.constraints.NotBlank;

public record CancelOrderRequest(
        @NotBlank String tenantId
) {
}