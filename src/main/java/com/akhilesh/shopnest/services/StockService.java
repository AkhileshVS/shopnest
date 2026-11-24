package com.akhilesh.shopnest.services;

import com.akhilesh.shopnest.exceptions.InsufficientStockException;
import com.akhilesh.shopnest.models.Order;
import com.akhilesh.shopnest.models.OrderItem;
import com.akhilesh.shopnest.models.Product;
import com.akhilesh.shopnest.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockService {

    private final ProductRepository productRepository;
    private final ProductCacheEvictor cacheEvictor;

    @Transactional(propagation = Propagation.MANDATORY)
    public void reserve(Product product, int quantity) {
        int updated = productRepository.reduceStock(product.getId(), quantity);
        if (updated == 0) {
            throw new InsufficientStockException(product.getName());
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void release(Order order) {
        for (OrderItem item : order.getItems()) {
            productRepository.addStock(item.getProduct().getId(), item.getQuantity());
        }
        evict(order);
    }

    public void evict(Order order) {
        List<Long> ids = order.getItems().stream().map(i -> i.getProduct().getId()).toList();
        cacheEvictor.evict(ids);
    }
}
