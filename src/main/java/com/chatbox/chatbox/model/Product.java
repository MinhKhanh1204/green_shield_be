package com.chatbox.chatbox.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_product_active_order", columnList = "active, display_order"),
        @Index(name = "idx_product_category", columnList = "category")
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, updatable = false, nullable = false)
    private String id;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(nullable = false, length = 180)
    private String nameVi;

    @Column(nullable = false, length = 180)
    private String nameEn;

    @Column(nullable = false, length = 500)
    private String shortDescriptionVi;

    @Column(nullable = false, length = 500)
    private String shortDescriptionEn;

    @Lob
    @Column(nullable = false)
    private String descriptionVi;

    @Lob
    @Column(nullable = false)
    private String descriptionEn;

    @Column(nullable = false, length = 255)
    private String materialVi;

    @Column(nullable = false, length = 255)
    private String materialEn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ProductCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ProductSaleMode saleMode;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal domesticUnitPrice;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal exportUnitPrice;

    private Integer comboQuantity;

    @Column(precision = 15, scale = 2)
    private BigDecimal domesticComboPrice;

    private Integer minimumOrderQuantity;

    @Builder.Default
    @Column(nullable = false)
    private boolean featured = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @ElementCollection
    @CollectionTable(name = "product_benefits_vi", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "value", length = 500)
    @OrderColumn(name = "sort_order")
    @Builder.Default
    private List<String> benefitsVi = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "product_benefits_en", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "value", length = 500)
    @OrderColumn(name = "sort_order")
    @Builder.Default
    private List<String> benefitsEn = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "product_applications_vi", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "value", length = 500)
    @OrderColumn(name = "sort_order")
    @Builder.Default
    private List<String> applicationsVi = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "product_applications_en", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "value", length = 500)
    @OrderColumn(name = "sort_order")
    @Builder.Default
    private List<String> applicationsEn = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "product_specifications_vi", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "value", length = 500)
    @OrderColumn(name = "sort_order")
    @Builder.Default
    private List<String> specificationsVi = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "product_specifications_en", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "value", length = 500)
    @OrderColumn(name = "sort_order")
    @Builder.Default
    private List<String> specificationsEn = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void addImage(ProductImage image) {
        images.add(image);
        image.setProduct(this);
    }

    public void removeImage(ProductImage image) {
        images.remove(image);
        image.setProduct(null);
    }
}
