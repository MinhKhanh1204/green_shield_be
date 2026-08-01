package com.chatbox.chatbox.dto;

import jakarta.validation.constraints.Min;

public final class ProductPatchRequests {
    private ProductPatchRequests() {
    }

    public record Status(boolean active) {
    }

    public record Featured(boolean featured) {
    }

    public record DisplayOrder(@Min(0) int displayOrder) {
    }
}
