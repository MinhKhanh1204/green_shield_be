package com.chatbox.chatbox.service.product;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;

public final class ProductImageFileValidator {
    public static final long MAX_BYTES = 5L * 1024L * 1024L;

    private ProductImageFileValidator() {
    }

    public static ValidatedImage validate(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("Image file must not exceed 5 MB");
        }
        byte[] content = file.getBytes();
        ImageFormat format = detect(content);
        return new ValidatedImage(content, file.getOriginalFilename(), format);
    }

    public static ImageFormat detect(byte[] content) {
        if (content == null || content.length < 12) {
            throw new IllegalArgumentException("Invalid image file");
        }
        if ((content[0] & 0xff) == 0xff && (content[1] & 0xff) == 0xd8 && (content[2] & 0xff) == 0xff) {
            return ImageFormat.JPEG;
        }
        byte[] png = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        if (Arrays.equals(Arrays.copyOf(content, 8), png)) {
            return ImageFormat.PNG;
        }
        if (new String(content, 0, 4).equals("RIFF") && new String(content, 8, 4).equals("WEBP")) {
            return ImageFormat.WEBP;
        }
        throw new IllegalArgumentException("Only JPEG, PNG, and WebP images are accepted");
    }

    public enum ImageFormat {
        JPEG("jpg", "jpeg"),
        PNG("png", "png"),
        WEBP("webp", "webp");

        private final String extension;
        private final String writerFormat;

        ImageFormat(String extension, String writerFormat) {
            this.extension = extension;
            this.writerFormat = writerFormat;
        }

        public String extension() {
            return extension;
        }

        public String writerFormat() {
            return writerFormat;
        }
    }

    public record ValidatedImage(byte[] content, String originalFilename, ImageFormat format) {
    }
}
