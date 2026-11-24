package com.akhilesh.shopnest.services;

import com.akhilesh.shopnest.config.RedisCacheConfig;
import com.akhilesh.shopnest.dtos.CategoryRequest;
import com.akhilesh.shopnest.dtos.CategoryResponse;
import com.akhilesh.shopnest.exceptions.BadRequestException;
import com.akhilesh.shopnest.exceptions.ResourceNotFoundException;
import com.akhilesh.shopnest.models.Category;
import com.akhilesh.shopnest.repositories.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Cacheable(value = RedisCacheConfig.CATEGORIES, key = "'all'")
    public List<CategoryResponse> getAll() {
        return new ArrayList<>(categoryRepository.findAll().stream().map(CategoryResponse::from).toList());
    }

    public CategoryResponse getById(Long id) {
        return CategoryResponse.from(find(id));
    }

    @CacheEvict(value = RedisCacheConfig.CATEGORIES, allEntries = true)
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.findByNameIgnoreCase(request.name()).isPresent()) {
            throw new BadRequestException("Category already exists");
        }
        Category category = new Category();
        category.setName(request.name());
        category.setDescription(request.description());
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @CacheEvict(value = RedisCacheConfig.CATEGORIES, allEntries = true)
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = find(id);
        category.setName(request.name());
        category.setDescription(request.description());
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @CacheEvict(value = RedisCacheConfig.CATEGORIES, allEntries = true)
    public void delete(Long id) {
        categoryRepository.delete(find(id));
    }

    Category find(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id " + id));
    }
}
