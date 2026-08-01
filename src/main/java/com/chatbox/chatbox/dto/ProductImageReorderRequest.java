package com.chatbox.chatbox.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ProductImageReorderRequest(@NotEmpty List<Long> imageIds) {
}
