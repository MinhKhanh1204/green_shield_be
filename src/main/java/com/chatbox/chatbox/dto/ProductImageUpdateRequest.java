package com.chatbox.chatbox.dto;

import com.chatbox.chatbox.model.ProductImageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductImageUpdateRequest(
        @NotBlank @Size(max = 300) String altTextVi,
        @NotBlank @Size(max = 300) String altTextEn,
        @NotNull ProductImageType imageType
) {
}
