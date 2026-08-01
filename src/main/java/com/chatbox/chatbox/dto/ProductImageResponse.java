package com.chatbox.chatbox.dto;

import com.chatbox.chatbox.model.ProductImageType;

import java.time.Instant;

public record ProductImageResponse(
        Long id,
        String imageUrl,
        String thumbnailUrl,
        String altTextVi,
        String altTextEn,
        ProductImageType imageType,
        int sortOrder,
        boolean mainImage,
        Instant createdAt
) {
}
