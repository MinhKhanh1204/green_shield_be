package com.chatbox.chatbox;

import com.chatbox.chatbox.model.Product;
import com.chatbox.chatbox.model.ProductCategory;
import com.chatbox.chatbox.model.ProductImage;
import com.chatbox.chatbox.model.ProductImageType;
import com.chatbox.chatbox.model.ProductSaleMode;
import com.chatbox.chatbox.repository.ProductImageRepository;
import com.chatbox.chatbox.repository.ProductRepository;
import com.chatbox.chatbox.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:product-catalog;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductCatalogIntegrationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private ProductService productService;

    @BeforeEach
    void cleanDatabase() {
        productImageRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void publicApiOnlyReturnsActiveProductsAndSupportsFilters() throws Exception {
        productRepository.save(product("lotus-plate", "Lotus plate", ProductCategory.TABLEWARE, true, true));
        productRepository.save(product("hidden-plate", "Hidden lotus plate", ProductCategory.TABLEWARE, false, true));
        productRepository.save(product("fruit-box", "Fruit box", ProductCategory.PACKAGING, true, false));

        mockMvc.perform(get("/api/v1/products")
                        .param("category", "TABLEWARE")
                        .param("featured", "true")
                        .param("search", "lotus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("lotus-plate"));

        mockMvc.perform(get("/api/v1/products").param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void adminApiRejectsAnonymousRequests() throws Exception {
        int responseStatus = mockMvc.perform(get("/api/v1/admin/products")
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getStatus();

        assertThat(responseStatus).isIn(302, 401, 403);
    }

    @Test
    @WithMockUser(username = "admin@example.com")
    void adminApiRejectsDuplicateSlugAndInvalidCommercialData() throws Exception {
        Map<String, Object> request = request("unique-product", ProductSaleMode.RETAIL);
        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Product slug already exists"));

        Map<String, Object> invalidPrice = request("invalid-price", ProductSaleMode.RETAIL);
        invalidPrice.put("domesticUnitPrice", -1);
        mockMvc.perform(post("/api/v1/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPrice)))
                .andExpect(status().isBadRequest());

        Map<String, Object> missingCombo = request("missing-combo", ProductSaleMode.COMBO);
        mockMvc.perform(post("/api/v1/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(missingCombo)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Combo quantity and combo price are required for combo products"));
    }

    @Test
    @Transactional
    void selectingMainImageLeavesExactlyOneMainImage() {
        Product product = product("image-product", "Image product", ProductCategory.LIFESTYLE, true, false);
        product.addImage(image("/one.webp", 0, true));
        product.addImage(image("/two.webp", 1, false));
        product = productRepository.saveAndFlush(product);
        Long selectedId = product.getImages().get(1).getId();

        productService.setMainImage(product.getId(), selectedId);

        Product saved = productRepository.findById(product.getId()).orElseThrow();
        assertThat(saved.getImages()).filteredOn(ProductImage::isMainImage).hasSize(1);
        assertThat(saved.getImages()).filteredOn(ProductImage::isMainImage)
                .extracting(ProductImage::getId)
                .containsExactly(selectedId);
    }

    private static Product product(
            String slug,
            String name,
            ProductCategory category,
            boolean active,
            boolean featured) {
        return Product.builder()
                .slug(slug)
                .nameVi(name)
                .nameEn(name)
                .shortDescriptionVi("Mo ta ngan")
                .shortDescriptionEn("Short description")
                .descriptionVi("Mo ta chi tiet")
                .descriptionEn("Detailed description")
                .materialVi("Luc binh")
                .materialEn("Water hyacinth")
                .category(category)
                .saleMode(ProductSaleMode.RETAIL)
                .domesticUnitPrice(new BigDecimal("2500"))
                .exportUnitPrice(new BigDecimal("5000"))
                .minimumOrderQuantity(1)
                .featured(featured)
                .active(active)
                .displayOrder(1)
                .benefitsVi(new ArrayList<>())
                .benefitsEn(new ArrayList<>())
                .applicationsVi(new ArrayList<>())
                .applicationsEn(new ArrayList<>())
                .specificationsVi(new ArrayList<>())
                .specificationsEn(new ArrayList<>())
                .build();
    }

    private static ProductImage image(String url, int order, boolean main) {
        return ProductImage.builder()
                .imageUrl(url)
                .thumbnailUrl(url)
                .storageKey(url)
                .altTextVi("Anh san pham")
                .altTextEn("Product image")
                .imageType(ProductImageType.GALLERY)
                .sortOrder(order)
                .mainImage(main)
                .build();
    }

    private static Map<String, Object> request(String slug, ProductSaleMode saleMode) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("slug", slug);
        request.put("nameVi", "San pham");
        request.put("nameEn", "Product");
        request.put("shortDescriptionVi", "Mo ta ngan");
        request.put("shortDescriptionEn", "Short description");
        request.put("descriptionVi", "Mo ta chi tiet");
        request.put("descriptionEn", "Detailed description");
        request.put("materialVi", "Luc binh");
        request.put("materialEn", "Water hyacinth");
        request.put("category", "PACKAGING");
        request.put("saleMode", saleMode.name());
        request.put("domesticUnitPrice", 2500);
        request.put("exportUnitPrice", 5000);
        request.put("minimumOrderQuantity", 1);
        request.put("featured", false);
        request.put("active", true);
        request.put("displayOrder", 1);
        request.put("benefitsVi", List.of());
        request.put("benefitsEn", List.of());
        request.put("applicationsVi", List.of());
        request.put("applicationsEn", List.of());
        request.put("specificationsVi", List.of());
        request.put("specificationsEn", List.of());
        return request;
    }
}
