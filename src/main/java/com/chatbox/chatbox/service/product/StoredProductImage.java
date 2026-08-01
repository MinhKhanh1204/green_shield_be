package com.chatbox.chatbox.service.product;

public record StoredProductImage(
        String imageUrl,
        String thumbnailUrl,
        String cloudinaryPublicId,
        String storageKey
) {
}
