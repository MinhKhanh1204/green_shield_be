package com.chatbox.chatbox.dto;

import com.chatbox.chatbox.model.ProductCategory;
import com.chatbox.chatbox.model.ProductSaleMode;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record ProductUpsertRequest(
        @NotBlank @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$") @Size(max = 120) String slug,
        @NotBlank @Size(max = 180) String nameVi,
        @NotBlank @Size(max = 180) String nameEn,
        @NotBlank @Size(max = 500) String shortDescriptionVi,
        @NotBlank @Size(max = 500) String shortDescriptionEn,
        @NotBlank String descriptionVi,
        @NotBlank String descriptionEn,
        @NotBlank @Size(max = 255) String materialVi,
        @NotBlank @Size(max = 255) String materialEn,
        @NotNull ProductCategory category,
        @NotNull ProductSaleMode saleMode,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal domesticUnitPrice,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal exportUnitPrice,
        @Positive Integer comboQuantity,
        @DecimalMin(value = "0.0", inclusive = false) BigDecimal domesticComboPrice,
        @Positive Integer minimumOrderQuantity,
        boolean featured,
        boolean active,
        @Min(0) int displayOrder,
        @Size(max = 12) List<@NotBlank @Size(max = 500) String> benefitsVi,
        @Size(max = 12) List<@NotBlank @Size(max = 500) String> benefitsEn,
        @Size(max = 12) List<@NotBlank @Size(max = 500) String> applicationsVi,
        @Size(max = 12) List<@NotBlank @Size(max = 500) String> applicationsEn,
        @Size(max = 12) List<@NotBlank @Size(max = 500) String> specificationsVi,
        @Size(max = 12) List<@NotBlank @Size(max = 500) String> specificationsEn
) {
}
