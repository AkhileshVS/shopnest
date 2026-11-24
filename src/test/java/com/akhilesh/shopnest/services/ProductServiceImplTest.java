package com.akhilesh.shopnest.services;

import com.akhilesh.shopnest.dtos.ProductRequest;
import com.akhilesh.shopnest.dtos.ProductResponse;
import com.akhilesh.shopnest.exceptions.BadRequestException;
import com.akhilesh.shopnest.exceptions.ResourceNotFoundException;
import com.akhilesh.shopnest.models.Category;
import com.akhilesh.shopnest.models.Product;
import com.akhilesh.shopnest.repositories.ProductRepository;
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
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void getProductReturnsMappedResponse() {
        Category category = new Category();
        category.setId(2L);
        category.setName("Electronics");

        Product product = new Product();
        product.setId(1L);
        product.setName("Headphones");
        product.setPrice(new BigDecimal("1999.00"));
        product.setStock(5);
        product.setCategory(category);

        when(productRepository.findWithCategoryById(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.getProduct(1L);

        assertThat(response.name()).isEqualTo("Headphones");
        assertThat(response.categoryName()).isEqualTo("Electronics");
    }

    @Test
    void getProductThrowsWhenMissing() {
        when(productRepository.findWithCategoryById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProduct(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rejectsUnknownSortField() {
        assertThatThrownBy(() -> productService.getProducts(0, 10, "password", "asc"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createProductSetsCategory() {
        Category category = new Category();
        category.setId(3L);
        category.setName("Books");
        when(categoryService.find(3L)).thenReturn(category);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductRequest request = new ProductRequest("Clean Code", "Book", new BigDecimal("499"), 10, null, 3L);
        ProductResponse response = productService.createProduct(request);

        assertThat(response.categoryId()).isEqualTo(3L);
        assertThat(response.stock()).isEqualTo(10);
    }
}
