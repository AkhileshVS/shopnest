package com.akhilesh.shopnest.controllers;

import com.akhilesh.shopnest.dtos.AddToCartRequest;
import com.akhilesh.shopnest.dtos.CartResponse;
import com.akhilesh.shopnest.security.UserPrincipal;
import com.akhilesh.shopnest.services.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public CartResponse getCart(@AuthenticationPrincipal UserPrincipal user) {
        return cartService.getCart(user.getId());
    }

    @PostMapping("/items")
    public CartResponse addItem(@AuthenticationPrincipal UserPrincipal user, @Valid @RequestBody AddToCartRequest request) {
        return cartService.addItem(user.getId(), request);
    }

    @PutMapping("/items/{productId}")
    public CartResponse updateQuantity(@AuthenticationPrincipal UserPrincipal user,
                                       @PathVariable Long productId,
                                       @RequestParam int quantity) {
        return cartService.updateQuantity(user.getId(), productId, quantity);
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse removeItem(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long productId) {
        return cartService.removeItem(user.getId(), productId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(@AuthenticationPrincipal UserPrincipal user) {
        cartService.clear(user.getId());
    }
}
