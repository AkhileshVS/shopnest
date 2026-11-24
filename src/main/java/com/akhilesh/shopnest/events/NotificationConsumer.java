package com.akhilesh.shopnest.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.kafka.order-topic}", groupId = "shopnest-notifications")
    public void consume(String message) throws Exception {
        OrderStatusChangedEvent event = objectMapper.readValue(message, OrderStatusChangedEvent.class);
        String text = switch (event.status()) {
            case PENDING_PAYMENT -> "Your order #" + event.orderId() + " is placed. Complete the payment to confirm it.";
            case CONFIRMED -> "Payment received. Order #" + event.orderId() + " is confirmed.";
            case SHIPPED -> "Order #" + event.orderId() + " has been shipped.";
            case DELIVERED -> "Order #" + event.orderId() + " was delivered.";
            case CANCELLED -> "Order #" + event.orderId() + " was cancelled.";
        };
        log.info("Email to {}: {}", event.email(), text);
    }
}
