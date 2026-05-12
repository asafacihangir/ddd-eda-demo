package org.phoenix.demo.ordermanagement.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.phoenix.demo.domain.common.result.Result;
import org.phoenix.demo.ordermanagement.application.orders.commands.cancelorder.CancelOrderCommand;
import org.phoenix.demo.ordermanagement.application.orders.commands.createorder.CreateOrderCommand;
import org.phoenix.demo.shared.cqrs.RequestDispatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = OrderController.class)
class OrderControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    RequestDispatcher dispatcher;

    @Test
    void placeOrder_returns201_whenSuccessful() throws Exception {
        when(dispatcher.dispatch(any(CreateOrderCommand.class)))
                .thenReturn(Result.success("aggregate-id-123"));

        mockMvc.perform(post("/api/orders")
                .contentType("application/json")
                .content("""
                        {
                          "tenantId": "tenant-1",
                          "orderId": "ORD-1",
                          "customerId": "cust-1",
                          "subtotalAmount": 100.00,
                          "discountAmount": 10.00,
                          "taxAmount": 18.00,
                          "currencyCode": "TRY"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value("aggregate-id-123"));
    }

    @Test
    void placeOrder_returns400_whenValidationFails() throws Exception {
        mockMvc.perform(post("/api/orders")
                .contentType("application/json")
                .content("""
                        {
                          "tenantId": "tenant-1",
                          "orderId": "",
                          "customerId": "cust-1",
                          "subtotalAmount": 100.00,
                          "discountAmount": 10.00,
                          "taxAmount": 18.00,
                          "currencyCode": "TRY"
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void placeOrder_returns400_whenTenantIdMissing() throws Exception {
        mockMvc.perform(post("/api/orders")
                .contentType("application/json")
                .content("""
                        {
                          "orderId": "ORD-1",
                          "customerId": "cust-1",
                          "subtotalAmount": 100.00,
                          "discountAmount": 10.00,
                          "taxAmount": 18.00,
                          "currencyCode": "TRY"
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelOrder_returns200_whenSuccessful() throws Exception {
        when(dispatcher.dispatch(any(CancelOrderCommand.class)))
                .thenReturn(Result.success(null));

        mockMvc.perform(post("/api/orders/some-id/cancel")
                .contentType("application/json")
                .content("{\"tenantId\":\"tenant-1\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void cancelOrder_returns404_whenNotFound() throws Exception {
        when(dispatcher.dispatch(any(CancelOrderCommand.class)))
                .thenReturn(Result.failure("Order not found: some-id"));

        mockMvc.perform(post("/api/orders/some-id/cancel")
                .contentType("application/json")
                .content("{\"tenantId\":\"tenant-1\"}"))
                .andExpect(status().isNotFound());
    }
}