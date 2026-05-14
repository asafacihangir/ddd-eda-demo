package org.phoenix.demo.ordermanagement.infra.servicebus;

import com.azure.spring.messaging.servicebus.implementation.core.annotation.ServiceBusListener;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.servicebus.consumer.enabled", havingValue = "true")
public class OrderEventsConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventsConsumer.class);

    private final ObjectProvider<BiConsumer<String, byte[]>> observer;

    public OrderEventsConsumer(ObjectProvider<BiConsumer<String, byte[]>> observer) {
        this.observer = observer;
    }

    @ServiceBusListener(
            destination = "${app.servicebus.consumer.topic-name}",
            group = "${app.servicebus.consumer.subscription-name}")
    public void onMessage(
            @Payload byte[] payload,
            @Header(name = "tenantId", required = false) String tenantId,
            @Header(name = "orderId", required = false) String orderId,
            @Header(name = "outboxItemId", required = false) String outboxItemId,
            @Header(name = "eventType", required = false) String eventType) {
        int payloadLength = payload == null ? 0 : payload.length;
        log.info("Consumed integration event tenantId={} orderId={} outboxItemId={} eventType={} payloadLength={} payload={}",
                tenantId, orderId, outboxItemId, eventType, payloadLength,
                payloadLength == 0 ? "" : new String(payload, StandardCharsets.UTF_8));
        observer.ifAvailable(o -> o.accept(outboxItemId, payload));
    }
}
