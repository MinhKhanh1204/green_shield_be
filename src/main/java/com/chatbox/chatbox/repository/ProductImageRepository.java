package com.chatbox.chatbox.repository;

import com.chatbox.chatbox.model.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    long countByProductId(String productId);
}
