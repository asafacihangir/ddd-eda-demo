package org.phoenix.demo.ordermanagement.api;

import jakarta.validation.Valid;
import java.net.URI;
import org.phoenix.demo.domain.common.result.Result;
import org.phoenix.demo.ordermanagement.application.orders.commands.cancelorder.CancelOrderCommand;
import org.phoenix.demo.ordermanagement.application.orders.commands.createorder.CreateOrderCommand;
import org.phoenix.demo.shared.cqrs.RequestDispatcher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ApiComponent
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final RequestDispatcher dispatcher;

    public OrderController(RequestDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @PostMapping
    public ResponseEntity<?> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        CreateOrderCommand command = new CreateOrderCommand(
                request.tenantId(),
                request.orderId(),
                request.customerId(),
                request.subtotalAmount(),
                request.discountAmount(),
                request.taxAmount(),
                request.currencyCode());

        Result<String, String> result = dispatcher.dispatch(command);
        if (result.isFailure()) {
            return ResponseEntity.badRequest().body(new ApiError(result.getError()));
        }

        String aggregateId = result.getValue();
        return ResponseEntity
                .created(URI.create("/api/orders/" + aggregateId))
                .body(new PlaceOrderResponse(aggregateId));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable String orderId,
                                         @Valid @RequestBody CancelOrderRequest request) {
        Result<Void, String> result = dispatcher.dispatch(new CancelOrderCommand(request.tenantId(), orderId));
        if (result.isFailure()) {
            String error = result.getError();
            if (error.startsWith("Order not found")) {
                return ResponseEntity.status(404).body(new ApiError(error));
            }
            if (error.contains("can be cancelled") || error.contains("UUID")) {
                return ResponseEntity.unprocessableEntity().body(new ApiError(error));
            }
            return ResponseEntity.badRequest().body(new ApiError(error));
        }
        return ResponseEntity.ok().build();
    }

    public record PlaceOrderResponse(String orderId) {
    }

    public record ApiError(String message) {
    }
}