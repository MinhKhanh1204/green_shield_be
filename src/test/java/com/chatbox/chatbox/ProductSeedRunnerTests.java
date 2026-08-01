package com.chatbox.chatbox;

import com.chatbox.chatbox.config.ProductSeedRunner;
import com.chatbox.chatbox.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:product-seed;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "app.product.seed-enabled=true",
        "app.product.upload-dir=build/test-product-seed",
        "app.product.public-base-url=/test-product-seed"
})
@ActiveProfiles("test")
class ProductSeedRunnerTests {
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductSeedRunner productSeedRunner;

    @Test
    @Transactional
    void seedsFiveProductsWithFiveImagesAndRemainsIdempotent() throws Exception {
        assertThat(productRepository.findAll()).hasSize(5)
                .allSatisfy(product -> assertThat(product.getImages()).hasSize(5));

        productSeedRunner.run(new DefaultApplicationArguments());

        assertThat(productRepository.findAll()).hasSize(5)
                .allSatisfy(product -> assertThat(product.getImages()).hasSize(5));
    }

    @Test
    @Transactional
    void restoresMissingLocalSeedFilesWithoutDuplicatingImageRecords() throws Exception {
        Path display = Path.of("build/test-product-seed/dia-la-sen/dia-la-sen-01.webp");
        Path thumbnail = Path.of("build/test-product-seed/dia-la-sen/dia-la-sen-01-thumb.webp");
        Files.deleteIfExists(display);
        Files.deleteIfExists(thumbnail);

        productSeedRunner.run(new DefaultApplicationArguments());

        assertThat(display).exists();
        assertThat(thumbnail).exists();
        assertThat(productRepository.findBySlug("dia-la-sen")).get()
                .extracting(product -> product.getImages().size())
                .isEqualTo(5);
    }
}
