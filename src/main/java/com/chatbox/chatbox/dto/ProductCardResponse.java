package com.chatbox.chatbox.dto;

import com.chatbox.chatbox.model.ProductCategory;
import com.chatbox.chatbox.model.ProductSaleMode;

import java.math.BigDecimal;

public record ProductCardResponse(
        String id,
        String slug,
        String nameVi,
        String nameEn,
        String shortDescriptionVi,
        String shortDescriptionEn,
        String materialVi,
        String materialEn,
        ProductCategory category,
        ProductSaleMode saleMode,
        BigDecimal domesticUnitPrice,
        BigDecimal exportUnitPrice,
        Integer comboQuantity,
        BigDecimal domesticComboPrice,
        boolean featured,
        boolean active,
        int displayOrder,
        ProductImageResponse mainImage
) {
}
