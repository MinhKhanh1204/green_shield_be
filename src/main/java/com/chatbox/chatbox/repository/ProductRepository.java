package com.chatbox.chatbox.repository;

import com.chatbox.chatbox.model.Product;
import com.chatbox.chatbox.model.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, String> {
    Optional<Product> findBySlug(String slug);
    Optional<Product> findBySlugAndActiveTrue(String slug);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, String id);
    List<Product> findAllByOrderByDisplayOrderAscCreatedAtDesc();
    List<Product> findAllByActiveTrueOrderByDisplayOrderAscCreatedAtDesc();
    List<Product> findTop4ByActiveTrueAndCategoryAndIdNotOrderByFeaturedDescDisplayOrderAsc(
            ProductCategory category, String id);
}
