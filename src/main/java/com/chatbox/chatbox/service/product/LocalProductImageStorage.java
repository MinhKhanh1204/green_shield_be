package com.chatbox.chatbox.service.product;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.product.image-storage", havingValue = "local", matchIfMissing = true)
public class LocalProductImageStorage implements ProductImageStorage {
    private final Path uploadRoot;
    private final String publicBaseUrl;

    public LocalProductImageStorage(
            @Value("${app.product.upload-dir:uploads/products}") String uploadDir,
            @Value("${app.product.public-base-url:/uploads/products}") String publicBaseUrl) {
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
    }

    @Override
    public StoredProductImage store(byte[] content, String originalFilename, String slug) throws IOException {
        ProductImageFileValidator.ImageFormat format = ProductImageFileValidator.detect(content);
        String safeSlug = sanitizeSegment(slug);
        String baseName = UUID.randomUUID().toString().toLowerCase(Locale.ROOT);
        String displayName = baseName + "." + format.extension();
        String thumbnailName = baseName + "-thumb." + format.extension();
        Path directory = resolveDirectory(safeSlug);
        Files.createDirectories(directory);

        if (format == ProductImageFileValidator.ImageFormat.WEBP) {
            write(directory.resolve(displayName), content);
            write(directory.resolve(thumbnailName), content);
        } else {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(content));
            if (source == null) throw new IllegalArgumentException("Image content cannot be decoded");
            writeScaled(source, directory.resolve(displayName), 1448, format);
            writeScaled(source, directory.resolve(thumbnailName), 640, format);
        }

        return stored(safeSlug, displayName, thumbnailName);
    }

    @Override
    public StoredProductImage storePrepared(
            byte[] displayContent,
            byte[] thumbnailContent,
            String fileName,
            String slug) throws IOException {
        ProductImageFileValidator.ImageFormat displayFormat = ProductImageFileValidator.detect(displayContent);
        ProductImageFileValidator.detect(thumbnailContent);
        String safeSlug = sanitizeSegment(slug);
        String safeBase = sanitizeBaseName(fileName);
        String displayName = safeBase + "." + displayFormat.extension();
        String thumbnailName = safeBase + "-thumb." + displayFormat.extension();
        Path directory = resolveDirectory(safeSlug);
        Files.createDirectories(directory);
        write(directory.resolve(displayName), displayContent);
        write(directory.resolve(thumbnailName), thumbnailContent);
        return stored(safeSlug, displayName, thumbnailName);
    }

    @Override
    public void delete(String storageKey, String cloudinaryPublicId) throws IOException {
        if (storageKey == null || storageKey.isBlank()) return;
        for (String relative : storageKey.split("\\|")) {
            Path target = uploadRoot.resolve(relative).normalize();
            if (!target.startsWith(uploadRoot)) {
                throw new IllegalArgumentException("Invalid product image path");
            }
            Files.deleteIfExists(target);
        }
    }

    private StoredProductImage stored(String slug, String displayName, String thumbnailName) {
        String displayRelative = slug + "/" + displayName;
        String thumbnailRelative = slug + "/" + thumbnailName;
        return new StoredProductImage(
                publicBaseUrl + "/" + displayRelative,
                publicBaseUrl + "/" + thumbnailRelative,
                null,
                displayRelative + "|" + thumbnailRelative
        );
    }

    private Path resolveDirectory(String slug) {
        Path directory = uploadRoot.resolve(slug).normalize();
        if (!directory.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("Invalid product slug path");
        }
        return directory;
    }

    private static void write(Path path, byte[] content) throws IOException {
        Files.write(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static void writeScaled(
            BufferedImage source,
            Path target,
            int maxWidth,
            ProductImageFileValidator.ImageFormat format) throws IOException {
        double scale = Math.min(1d, (double) maxWidth / source.getWidth());
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        int imageType = format == ProductImageFileValidator.ImageFormat.JPEG
                ? BufferedImage.TYPE_INT_RGB
                : BufferedImage.TYPE_INT_ARGB;
        BufferedImage output = new BufferedImage(width, height, imageType);
        Graphics2D graphics = output.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        if (imageType == BufferedImage.TYPE_INT_RGB) {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
        }
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();
        if (!ImageIO.write(output, format.writerFormat(), target.toFile())) {
            throw new IOException("No image writer available for " + format.writerFormat());
        }
    }

    private static String sanitizeSegment(String value) {
        if (value == null || !value.matches("^[a-z0-9]+(?:-[a-z0-9]+)*$")) {
            throw new IllegalArgumentException("Invalid product slug");
        }
        return value;
    }

    private static String sanitizeBaseName(String value) {
        String base = value == null ? "product-image" : value.replaceFirst("\\.[^.]+$", "");
        base = base.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]+", "-").replaceAll("^-+|-+$", "");
        return base.isBlank() ? "product-image" : base;
    }
}
