package com.chatbox.chatbox;

import com.chatbox.chatbox.service.product.ProductImageFileValidator;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductImageFileValidatorTests {
    @Test
    void rejectsSpoofedMimeType() {
        MockMultipartFile file = new MockMultipartFile(
                "files", "fake.png", "image/png", "not-an-image".getBytes());

        assertThatThrownBy(() -> ProductImageFileValidator.validate(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only JPEG, PNG, and WebP images are accepted");
    }

    @Test
    void rejectsFilesLargerThanFiveMegabytes() {
        byte[] oversized = new byte[(int) ProductImageFileValidator.MAX_BYTES + 1];
        MockMultipartFile file = new MockMultipartFile(
                "files", "large.webp", "image/webp", oversized);

        assertThatThrownBy(() -> ProductImageFileValidator.validate(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Image file must not exceed 5 MB");
    }
}
