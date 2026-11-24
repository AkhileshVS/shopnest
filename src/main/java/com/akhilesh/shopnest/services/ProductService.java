package com.akhilesh.shopnest.services;

import com.akhilesh.shopnest.dtos.PageResponse;
import com.akhilesh.shopnest.dtos.ProductRequest;
import com.akhilesh.shopnest.dtos.ProductResponse;

public interface ProductService {

    ProductResponse getProduct(Long id);

    PageResponse<ProductResponse> getProducts(int page, int size, String sortBy, String direction);

    PageResponse<ProductResponse> getProductsByCategory(Long categoryId, int page, int size);

    PageResponse<ProductResponse> search(String query, int page, int size);

    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(Long id, ProductRequest request);

    void deleteProduct(Long id);
}
