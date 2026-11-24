package com.akhilesh.shopnest.services;

import com.akhilesh.shopnest.dtos.CheckoutResponse;
import com.akhilesh.shopnest.dtos.OrderResponse;
import com.akhilesh.shopnest.dtos.PageResponse;
import com.akhilesh.shopnest.dtos.PlaceOrderRequest;
import com.akhilesh.shopnest.events.OrderStatusChangedEvent;
import com.akhilesh.shopnest.exceptions.BadRequestException;
import com.akhilesh.shopnest.exceptions.ResourceNotFoundException;
import com.akhilesh.shopnest.models.*;
import com.akhilesh.shopnest.repositories.CartRepository;
import com.akhilesh.shopnest.repositories.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartService cartService;
    private final StockService stockService;
    private final PaymentService paymentService;
    private final ApplicationEventPublisher events;

    @Transactional
    public CheckoutResponse checkout(Long userId, PlaceOrderRequest request) {
        Cart cart = cartService.getOrCreate(userId);
        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        Order order = new Order();
        order.setUser(cart.getUser());
        order.setShippingAddress(request.shippingAddress());
        order.setStatus(OrderStatus.PENDING_PAYMENT);

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            stockService.reserve(product, cartItem.getQuantity());

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(cartItem.getQuantity());
            item.setPriceAtPurchase(product.getPrice());
            order.getItems().add(item);

            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }
        order.setTotalAmount(total);
        order = orderRepository.save(order);

        cart.getItems().clear();
        cartRepository.save(cart);
        stockService.evict(order);

        Payment payment = paymentService.initiate(order, request.gateway());
        events.publishEvent(new OrderStatusChangedEvent(order.getId(), userId, order.getUser().getEmail(),
                order.getStatus(), total));

        return new CheckoutResponse(order.getId(), total, payment.getReferenceId(), payment.getPaymentLink());
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getMyOrders(Long userId, int page, int size) {
        var result = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, Math.min(size, 50)));
        return PageResponse.of(result, OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long userId, Long orderId) {
        return OrderResponse.from(findForUser(userId, orderId));
    }

    @Transactional
    public OrderResponse cancel(Long userId, Long orderId) {
        Order order = findForUser(userId, orderId);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BadRequestException("Order cannot be cancelled in status " + order.getStatus());
        }
        order.setStatus(OrderStatus.CANCELLED);
        stockService.release(order);
        publish(order);
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse updateStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id " + orderId));
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Order is already cancelled");
        }
        order.setStatus(status);
        publish(order);
        return OrderResponse.from(orderRepository.save(order));
    }

    private Order findForUser(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id " + orderId));
    }

    private void publish(Order order) {
        events.publishEvent(new OrderStatusChangedEvent(order.getId(), order.getUser().getId(),
                order.getUser().getEmail(), order.getStatus(), order.getTotalAmount()));
    }
}
