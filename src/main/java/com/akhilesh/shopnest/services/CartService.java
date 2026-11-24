package com.akhilesh.shopnest.services;

import com.akhilesh.shopnest.dtos.AddToCartRequest;
import com.akhilesh.shopnest.dtos.CartResponse;
import com.akhilesh.shopnest.exceptions.BadRequestException;
import com.akhilesh.shopnest.exceptions.ResourceNotFoundException;
import com.akhilesh.shopnest.models.Cart;
import com.akhilesh.shopnest.models.CartItem;
import com.akhilesh.shopnest.models.Product;
import com.akhilesh.shopnest.models.User;
import com.akhilesh.shopnest.repositories.CartRepository;
import com.akhilesh.shopnest.repositories.ProductRepository;
import com.akhilesh.shopnest.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public CartResponse getCart(Long userId) {
        return CartResponse.from(getOrCreate(userId));
    }

    @Transactional
    public CartResponse addItem(Long userId, AddToCartRequest request) {
        Cart cart = getOrCreate(userId);
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + request.productId()));

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(product.getId()))
                .findFirst()
                .orElseGet(() -> {
                    CartItem newItem = new CartItem();
                    newItem.setCart(cart);
                    newItem.setProduct(product);
                    cart.getItems().add(newItem);
                    return newItem;
                });

        int newQty = item.getQuantity() + request.quantity();
        if (product.getStock() == null || product.getStock() < newQty) {
            throw new BadRequestException("Only " + product.getStock() + " left for " + product.getName());
        }
        item.setQuantity(newQty);
        return CartResponse.from(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse updateQuantity(Long userId, Long productId, int quantity) {
        Cart cart = getOrCreate(userId);
        CartItem item = findItem(cart, productId);
        if (quantity <= 0) {
            cart.getItems().remove(item);
        } else {
            if (item.getProduct().getStock() < quantity) {
                throw new BadRequestException("Only " + item.getProduct().getStock() + " left");
            }
            item.setQuantity(quantity);
        }
        return CartResponse.from(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse removeItem(Long userId, Long productId) {
        Cart cart = getOrCreate(userId);
        cart.getItems().remove(findItem(cart, productId));
        return CartResponse.from(cartRepository.save(cart));
    }

    @Transactional
    public void clear(Long userId) {
        Cart cart = getOrCreate(userId);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    Cart getOrCreate(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            Cart cart = new Cart();
            cart.setUser(user);
            return cartRepository.save(cart);
        });
    }

    private CartItem findItem(Cart cart, Long productId) {
        return cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Product " + productId + " is not in cart"));
    }
}
