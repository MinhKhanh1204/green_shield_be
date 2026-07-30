package com.chatbox.chatbox.dto;

import com.chatbox.chatbox.model.ProductCategory;
import com.chatbox.chatbox.model.ProductSaleMode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductDetailResponse(
        String id,
        String slug,
        String nameVi,
        String nameEn,
        String shortDescriptionVi,
        String shortDescriptionEn,
        String descriptionVi,
        String descriptionEn,
        String materialVi,
        String materialEn,
        ProductCategory category,
        ProductSaleMode saleMode,
        BigDecimal domesticUnitPrice,
        BigDecimal exportUnitPrice,
        Integer comboQuantity,
        BigDecimal domesticComboPrice,
        Integer minimumOrderQuantity,
        boolean featured,
        boolean active,
        int displayOrder,
        List<String> benefitsVi,
        List<String> benefitsEn,
        List<String> applicationsVi,
        List<String> applicationsEn,
        List<String> specificationsVi,
        List<String> specificationsEn,
        List<ProductImageResponse> images,
        List<ProductCardResponse> relatedProducts,
        Instant createdAt,
        Instant updatedAt
) {
}
