package org.phoenix.demo.ordermanagement.application.orders.queries.getorderbyid;

import jakarta.validation.constraints.NotBlank;
import org.phoenix.demo.domain.common.result.Result;
import org.phoenix.demo.shared.cqrs.Query;
import org.phoenix.demo.ordermanagement.application.orders.queries.OrderSummaryDto;

public record GetOrderByIdQuery(
        @NotBlank String id
) implements Query<Result<OrderSummaryDto, String>> {
}