package com.chatbox.chatbox.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "product_images", indexes = {
        @Index(name = "idx_product_image_order", columnList = "product_id, sort_order")
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ProductImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String imageUrl;

    @Column(nullable = false, length = 1000)
    private String thumbnailUrl;

    @Column(length = 500)
    private String cloudinaryPublicId;

    @Column(length = 1000)
    private String storageKey;

    @Column(nullable = false, length = 300)
    private String altTextVi;

    @Column(nullable = false, length = 300)
    private String altTextEn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ProductImageType imageType;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Builder.Default
    @Column(nullable = false)
    private boolean mainImage = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
