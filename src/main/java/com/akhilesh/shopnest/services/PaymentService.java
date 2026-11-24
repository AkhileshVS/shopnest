package com.akhilesh.shopnest.services;

import com.akhilesh.shopnest.events.OrderStatusChangedEvent;
import com.akhilesh.shopnest.exceptions.BadRequestException;
import com.akhilesh.shopnest.exceptions.ResourceNotFoundException;
import com.akhilesh.shopnest.models.*;
import com.akhilesh.shopnest.repositories.PaymentRepository;
import com.akhilesh.shopnest.services.payment.PaymentGateway;
import com.akhilesh.shopnest.services.payment.WebhookResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final StockService stockService;
    private final ApplicationEventPublisher events;
    private final Map<PaymentGatewayType, PaymentGateway> gateways = new EnumMap<>(PaymentGatewayType.class);
    private final PaymentGatewayType defaultGateway;

    public PaymentService(PaymentRepository paymentRepository,
                          StockService stockService,
                          ApplicationEventPublisher events,
                          List<PaymentGateway> gatewayList,
                          @Value("${app.payment.default-gateway}") String defaultGateway) {
        this.paymentRepository = paymentRepository;
        this.stockService = stockService;
        this.events = events;
        gatewayList.forEach(g -> gateways.put(g.type(), g));
        this.defaultGateway = PaymentGatewayType.valueOf(defaultGateway.toUpperCase());
    }

    @Transactional
    public Payment initiate(Order order, PaymentGatewayType requested) {
        PaymentGatewayType type = requested != null ? requested : defaultGateway;

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(order.getTotalAmount());
        payment.setGateway(type);
        payment.setStatus(PaymentStatus.CREATED);
        payment.setReferenceId(UUID.randomUUID().toString());
        payment.setPaymentLink(gateway(type).createPaymentLink(payment, order.getUser()));
        return paymentRepository.save(payment);
    }

    @Transactional
    public void handleWebhook(PaymentGatewayType type, String payload, String signature) {
        WebhookResult result = gateway(type).parseWebhook(payload, signature).orElse(null);
        if (result == null) {
            return;
        }

        Payment payment = paymentRepository.findByReferenceId(result.referenceId())
                .orElseThrow(() -> new ResourceNotFoundException("Unknown payment reference " + result.referenceId()));

        if (payment.getStatus() != PaymentStatus.CREATED) {
            log.info("Payment {} already processed, ignoring duplicate webhook", payment.getReferenceId());
            return;
        }

        Order order = payment.getOrder();
        if (order.getStatus() == OrderStatus.CANCELLED) {
            payment.setStatus(result.success() ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
            payment.setGatewayPaymentId(result.gatewayPaymentId());
            paymentRepository.save(payment);
            if (result.success()) {
                log.warn("Payment {} received for cancelled order {}, refund required", payment.getReferenceId(), order.getId());
            }
            return;
        }

        if (result.success()) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setGatewayPaymentId(result.gatewayPaymentId());
            order.setStatus(OrderStatus.CONFIRMED);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            order.setStatus(OrderStatus.CANCELLED);
            stockService.release(order);
        }
        paymentRepository.save(payment);
        events.publishEvent(new OrderStatusChangedEvent(order.getId(), order.getUser().getId(),
                order.getUser().getEmail(), order.getStatus(), order.getTotalAmount()));
    }

    private PaymentGateway gateway(PaymentGatewayType type) {
        PaymentGateway gateway = gateways.get(type);
        if (gateway == null) {
            throw new BadRequestException("Unsupported payment gateway " + type);
        }
        return gateway;
    }
}
