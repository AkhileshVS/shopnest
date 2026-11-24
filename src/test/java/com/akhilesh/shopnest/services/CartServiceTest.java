package com.akhilesh.shopnest.services;

import com.akhilesh.shopnest.dtos.AddToCartRequest;
import com.akhilesh.shopnest.dtos.CartResponse;
import com.akhilesh.shopnest.exceptions.BadRequestException;
import com.akhilesh.shopnest.models.Cart;
import com.akhilesh.shopnest.models.Product;
import com.akhilesh.shopnest.models.User;
import com.akhilesh.shopnest.repositories.CartRepository;
import com.akhilesh.shopnest.repositories.ProductRepository;
import com.akhilesh.shopnest.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartService cartService;

    private Cart cart;
    private Product product;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(1L);
        cart = new Cart();
        cart.setId(10L);
        cart.setUser(user);

        product = new Product();
        product.setId(5L);
        product.setName("Keyboard");
        product.setPrice(new BigDecimal("1500"));
        product.setStock(3);
    }

    @Test
    void addingSameProductTwiceIncreasesQuantity() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        cartService.addItem(1L, new AddToCartRequest(5L, 1));
        CartResponse response = cartService.addItem(1L, new AddToCartRequest(5L, 2));

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quantity()).isEqualTo(3);
        assertThat(response.total()).isEqualByComparingTo("4500");
    }

    @Test
    void cannotAddMoreThanStock() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> cartService.addItem(1L, new AddToCartRequest(5L, 4)))
                .isInstanceOf(BadRequestException.class);
    }
}
