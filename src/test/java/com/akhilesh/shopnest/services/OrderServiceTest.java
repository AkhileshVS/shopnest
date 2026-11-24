package com.akhilesh.shopnest.services;

import com.akhilesh.shopnest.dtos.CheckoutResponse;
import com.akhilesh.shopnest.dtos.PlaceOrderRequest;
import com.akhilesh.shopnest.exceptions.BadRequestException;
import com.akhilesh.shopnest.exceptions.InsufficientStockException;
import com.akhilesh.shopnest.models.*;
import com.akhilesh.shopnest.repositories.CartRepository;
import com.akhilesh.shopnest.repositories.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartService cartService;
    @Mock
    private StockService stockService;
    @Mock
    private PaymentService paymentService;
    @Mock
    private ApplicationEventPublisher events;

    @InjectMocks
    private OrderService orderService;

    private Cart cart;
    private Product product;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        cart = new Cart();
        cart.setUser(user);

        product = new Product();
        product.setId(7L);
        product.setName("Mouse");
        product.setPrice(new BigDecimal("700"));
        product.setStock(10);

        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(2);
        cart.getItems().add(item);
    }

    @Test
    void checkoutCreatesOrderAndClearsCart() {
        when(cartService.getOrCreate(1L)).thenReturn(cart);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(100L);
            return o;
        });
        Payment payment = new Payment();
        payment.setReferenceId("ref-1");
        payment.setPaymentLink("https://rzp.io/test");
        when(paymentService.initiate(any(Order.class), eq(PaymentGatewayType.RAZORPAY))).thenReturn(payment);

        CheckoutResponse response = orderService.checkout(1L, new PlaceOrderRequest("Hyderabad", PaymentGatewayType.RAZORPAY));

        assertThat(response.orderId()).isEqualTo(100L);
        assertThat(response.amount()).isEqualByComparingTo("1400");
        assertThat(response.paymentLink()).isEqualTo("https://rzp.io/test");
        assertThat(cart.getItems()).isEmpty();
        verify(stockService).reserve(product, 2);
    }

    @Test
    void checkoutFailsWhenStockRunsOut() {
        when(cartService.getOrCreate(1L)).thenReturn(cart);
        doThrow(new InsufficientStockException("Mouse")).when(stockService).reserve(product, 2);

        assertThatThrownBy(() -> orderService.checkout(1L, new PlaceOrderRequest("Hyderabad", null)))
                .isInstanceOf(InsufficientStockException.class);
        verify(paymentService, never()).initiate(any(), any());
    }

    @Test
    void checkoutFailsForEmptyCart() {
        cart.getItems().clear();
        when(cartService.getOrCreate(1L)).thenReturn(cart);

        assertThatThrownBy(() -> orderService.checkout(1L, new PlaceOrderRequest("Hyderabad", null)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void cannotCancelShippedOrder() {
        Order order = new Order();
        order.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancel(1L, 5L)).isInstanceOf(BadRequestException.class);
    }
}
