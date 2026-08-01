package com.chatbox.chatbox.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
@ConditionalOnProperty(name = "app.product.image-storage", havingValue = "local", matchIfMissing = true)
public class ProductStorageWebConfig implements WebMvcConfigurer {
    private final String resourceLocation;

    public ProductStorageWebConfig(@Value("${app.product.upload-dir:uploads/products}") String uploadDir) {
        String uri = Path.of(uploadDir).toAbsolutePath().normalize().toUri().toString();
        this.resourceLocation = uri.endsWith("/") ? uri : uri + "/";
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/products/**")
                .addResourceLocations(resourceLocation)
                .setCachePeriod(31536000);
    }
}
