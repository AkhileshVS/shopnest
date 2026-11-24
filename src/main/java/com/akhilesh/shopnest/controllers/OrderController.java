package com.akhilesh.shopnest.controllers;

import com.akhilesh.shopnest.dtos.CheckoutResponse;
import com.akhilesh.shopnest.dtos.OrderResponse;
import com.akhilesh.shopnest.dtos.PageResponse;
import com.akhilesh.shopnest.dtos.PlaceOrderRequest;
import com.akhilesh.shopnest.models.OrderStatus;
import com.akhilesh.shopnest.security.UserPrincipal;
import com.akhilesh.shopnest.services.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public CheckoutResponse checkout(@AuthenticationPrincipal UserPrincipal user,
                                     @Valid @RequestBody PlaceOrderRequest request) {
        return orderService.checkout(user.getId(), request);
    }

    @GetMapping
    public PageResponse<OrderResponse> myOrders(@AuthenticationPrincipal UserPrincipal user,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        return orderService.getMyOrders(user.getId(), page, size);
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long id) {
        return orderService.getOrder(user.getId(), id);
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long id) {
        return orderService.cancel(user.getId(), id);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public OrderResponse updateStatus(@PathVariable Long id, @RequestParam OrderStatus status) {
        return orderService.updateStatus(id, status);
    }
}
