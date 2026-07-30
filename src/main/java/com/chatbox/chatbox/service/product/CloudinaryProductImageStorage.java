package com.chatbox.chatbox.service.product;

import com.chatbox.chatbox.service.CloudinaryService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.product.image-storage", havingValue = "cloudinary")
public class CloudinaryProductImageStorage implements ProductImageStorage {
    private final CloudinaryService cloudinaryService;

    public CloudinaryProductImageStorage(CloudinaryService cloudinaryService) {
        this.cloudinaryService = cloudinaryService;
    }

    @Override
    public StoredProductImage store(byte[] content, String originalFilename, String slug) throws IOException {
        ProductImageFileValidator.detect(content);
        return upload(content, UUID.randomUUID().toString().toLowerCase(Locale.ROOT), slug);
    }

    @Override
    public StoredProductImage storePrepared(
            byte[] displayContent,
            byte[] thumbnailContent,
            String fileName,
            String slug) throws IOException {
        ProductImageFileValidator.detect(displayContent);
        return upload(displayContent, sanitizeBaseName(fileName), slug);
    }

    @Override
    public void delete(String storageKey, String cloudinaryPublicId) throws IOException {
        cloudinaryService.deleteImage(cloudinaryPublicId != null ? cloudinaryPublicId : storageKey);
    }

    private StoredProductImage upload(byte[] content, String fileName, String slug) throws IOException {
        String folder = "greenshield/products/" + slug;
        Map<String, String> uploaded = cloudinaryService.uploadProductImage(content, fileName, folder);
        String sourceUrl = uploaded.get("secureUrl");
        String publicId = uploaded.get("publicId");
        return new StoredProductImage(
                transform(sourceUrl, "c_limit,w_1448,q_auto,f_auto"),
                transform(sourceUrl, "c_limit,w_640,q_auto,f_auto"),
                publicId,
                publicId
        );
    }

    private static String transform(String url, String transformation) {
        return url.replace("/upload/", "/upload/" + transformation + "/");
    }

    private static String sanitizeBaseName(String value) {
        String base = value == null ? "product-image" : value.replaceFirst("\\.[^.]+$", "");
        base = base.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]+", "-").replaceAll("^-+|-+$", "");
        return base.isBlank() ? "product-image" : base;
    }
}
