package com.akhilesh.shopnest.repositories;

import com.akhilesh.shopnest.models.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = "category")
    Optional<Product> findWithCategoryById(Long id);

    @EntityGraph(attributePaths = "category")
    Page<Product> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Modifying
    @Query("update Product p set p.stock = p.stock - :qty where p.id = :id and p.stock >= :qty")
    int reduceStock(@Param("id") Long id, @Param("qty") int qty);

    @Modifying
    @Query("update Product p set p.stock = p.stock + :qty where p.id = :id")
    int addStock(@Param("id") Long id, @Param("qty") int qty);
}
