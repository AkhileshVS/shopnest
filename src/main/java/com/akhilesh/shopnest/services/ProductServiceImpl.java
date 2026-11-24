package com.akhilesh.shopnest.services;

import com.akhilesh.shopnest.config.RedisCacheConfig;
import com.akhilesh.shopnest.dtos.PageResponse;
import com.akhilesh.shopnest.dtos.ProductRequest;
import com.akhilesh.shopnest.dtos.ProductResponse;
import com.akhilesh.shopnest.exceptions.BadRequestException;
import com.akhilesh.shopnest.exceptions.ResourceNotFoundException;
import com.akhilesh.shopnest.models.Product;
import com.akhilesh.shopnest.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final Set<String> SORTABLE = Set.of("id", "name", "price", "createdAt");
    private static final int MAX_PAGE_SIZE = 50;

    private final ProductRepository productRepository;
    private final CategoryService categoryService;

    @Override
    @Cacheable(value = RedisCacheConfig.PRODUCT, key = "#id")
    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        return ProductResponse.from(findProduct(id));
    }

    @Override
    @Cacheable(value = RedisCacheConfig.PRODUCT_PAGES, key = "#page + '-' + #size + '-' + #sortBy + '-' + #direction")
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProducts(int page, int size, String sortBy, String direction) {
        if (!SORTABLE.contains(sortBy)) {
            throw new BadRequestException("Cannot sort by " + sortBy);
        }
        Sort sort = "desc".equalsIgnoreCase(direction) ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        var result = productRepository.findAll(PageRequest.of(page, clamp(size), sort));
        return PageResponse.of(result, ProductResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProductsByCategory(Long categoryId, int page, int size) {
        categoryService.find(categoryId);
        var result = productRepository.findByCategoryId(categoryId, PageRequest.of(page, clamp(size)));
        return PageResponse.of(result, ProductResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(String query, int page, int size) {
        var result = productRepository.findByNameContainingIgnoreCase(query.trim(), PageRequest.of(page, clamp(size)));
        return PageResponse.of(result, ProductResponse::from);
    }

    @Override
    @CacheEvict(value = RedisCacheConfig.PRODUCT_PAGES, allEntries = true)
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = new Product();
        apply(product, request);
        return ProductResponse.from(productRepository.save(product));
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = RedisCacheConfig.PRODUCT, key = "#id"),
            @CacheEvict(value = RedisCacheConfig.PRODUCT_PAGES, allEntries = true)
    })
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = findProduct(id);
        apply(product, request);
        return ProductResponse.from(productRepository.save(product));
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = RedisCacheConfig.PRODUCT, key = "#id"),
            @CacheEvict(value = RedisCacheConfig.PRODUCT_PAGES, allEntries = true)
    })
    @Transactional
    public void deleteProduct(Long id) {
        productRepository.delete(findProduct(id));
    }

    private Product findProduct(Long id) {
        return productRepository.findWithCategoryById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + id));
    }

    private void apply(Product product, ProductRequest request) {
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setImageUrl(request.imageUrl());
        product.setCategory(categoryService.find(request.categoryId()));
    }

    private int clamp(int size) {
        return Math.max(1, Math.min(size, MAX_PAGE_SIZE));
    }
}
