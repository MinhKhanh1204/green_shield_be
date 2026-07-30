package com.chatbox.chatbox.config;

import com.chatbox.chatbox.model.*;
import com.chatbox.chatbox.repository.ProductRepository;
import com.chatbox.chatbox.service.product.ProductImageStorage;
import com.chatbox.chatbox.service.product.StoredProductImage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@Component
@ConditionalOnProperty(name = "app.product.seed-enabled", havingValue = "true")
public class ProductSeedRunner implements ApplicationRunner {
    private static final String MANIFEST = "classpath:product-seed/products-images.manifest.json";

    private final ProductRepository productRepository;
    private final ProductImageStorage imageStorage;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    public ProductSeedRunner(
            ProductRepository productRepository,
            ProductImageStorage imageStorage,
            ResourceLoader resourceLoader,
            ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.imageStorage = imageStorage;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        Map<String, List<ImageSeed>> manifest = readManifest();
        for (Product seed : products()) {
            Product product = productRepository.findBySlug(seed.getSlug()).orElseGet(Product::new);
            copySeedFields(seed, product);
            product = productRepository.save(product);

            if (product.getImages().isEmpty()) {
                for (ImageSeed imageSeed : manifest.getOrDefault(product.getSlug(), List.of())) {
                    if (!imageSeed.seed()) continue;
                    byte[] display = readResource(imageSeed.displayResource());
                    byte[] thumbnail = readResource(imageSeed.thumbnailResource());
                    StoredProductImage stored = imageStorage.storePrepared(
                            display, thumbnail, imageSeed.fileName(), product.getSlug());
                    product.addImage(ProductImage.builder()
                            .imageUrl(stored.imageUrl())
                            .thumbnailUrl(stored.thumbnailUrl())
                            .cloudinaryPublicId(stored.cloudinaryPublicId())
                            .storageKey(stored.storageKey())
                            .altTextVi(imageSeed.altTextVi())
                            .altTextEn(imageSeed.altTextEn())
                            .imageType(imageSeed.imageType())
                            .sortOrder(imageSeed.sortOrder())
                            .mainImage(imageSeed.mainImage())
                            .build());
                }
                productRepository.save(product);
            }
        }
    }

    private Map<String, List<ImageSeed>> readManifest() throws IOException {
        try (var stream = resourceLoader.getResource(MANIFEST).getInputStream()) {
            return objectMapper.readValue(stream, new TypeReference<>() {
            });
        }
    }

    private byte[] readResource(String path) throws IOException {
        try (var stream = resourceLoader.getResource("classpath:" + path).getInputStream()) {
            return stream.readAllBytes();
        }
    }

    private static void copySeedFields(Product source, Product target) {
        target.setSlug(source.getSlug());
        target.setNameVi(source.getNameVi());
        target.setNameEn(source.getNameEn());
        target.setShortDescriptionVi(source.getShortDescriptionVi());
        target.setShortDescriptionEn(source.getShortDescriptionEn());
        target.setDescriptionVi(source.getDescriptionVi());
        target.setDescriptionEn(source.getDescriptionEn());
        target.setMaterialVi(source.getMaterialVi());
        target.setMaterialEn(source.getMaterialEn());
        target.setCategory(source.getCategory());
        target.setSaleMode(source.getSaleMode());
        target.setDomesticUnitPrice(source.getDomesticUnitPrice());
        target.setExportUnitPrice(source.getExportUnitPrice());
        target.setComboQuantity(source.getComboQuantity());
        target.setDomesticComboPrice(source.getDomesticComboPrice());
        target.setMinimumOrderQuantity(source.getMinimumOrderQuantity());
        target.setFeatured(source.isFeatured());
        target.setActive(source.isActive());
        target.setDisplayOrder(source.getDisplayOrder());
        target.setBenefitsVi(new ArrayList<>(source.getBenefitsVi()));
        target.setBenefitsEn(new ArrayList<>(source.getBenefitsEn()));
        target.setApplicationsVi(new ArrayList<>(source.getApplicationsVi()));
        target.setApplicationsEn(new ArrayList<>(source.getApplicationsEn()));
        target.setSpecificationsVi(new ArrayList<>(source.getSpecificationsVi()));
        target.setSpecificationsEn(new ArrayList<>(source.getSpecificationsEn()));
    }

    private static List<Product> products() {
        return List.of(
                product(
                        "hop-dung-trai-cay", "Hộp đựng trái cây", "Fruit storage box",
                        "Giải pháp đóng gói nông sản từ sợi lục bình ép khuôn.",
                        "A molded water hyacinth fiber packaging solution for fresh produce.",
                        "Hộp nhẹ, chắc và có bề mặt vật liệu tự nhiên, phù hợp đóng gói trái cây nội địa và đơn hàng xuất khẩu.",
                        "A lightweight, sturdy box with a natural material finish for domestic produce and export programs.",
                        "Sợi lục bình ép khuôn", "Molded water hyacinth fiber", ProductCategory.PACKAGING,
                        ProductSaleMode.COMBO_AND_B2B, "2500", "5000", 50, "125000", 50, true, 1,
                        List.of("Giảm nhựa dùng một lần", "Tận dụng nguyên liệu bản địa", "Nhẹ và dễ xếp chồng"),
                        List.of("Reduces single-use plastic", "Uses local plant fiber", "Lightweight and stackable"),
                        List.of("Đóng gói xoài và trái cây", "Quà tặng nông sản", "Đơn hàng B2B"),
                        List.of("Mango and fruit packaging", "Produce gift sets", "B2B orders"),
                        List.of("Bán theo combo 50 hộp", "Có thể tùy chỉnh cho doanh nghiệp"),
                        List.of("Sold in sets of 50", "Business customization available")),
                product(
                        "dia-la-sen", "Dĩa lá sen", "Lotus leaf plate",
                        "Dĩa sinh học mang đường gân tự nhiên của lá sen.",
                        "A bio-based plate that preserves the natural veins of lotus leaves.",
                        "Dĩa lá sen ép định hình dành cho bàn ăn, catering và sự kiện theo phong cách vật liệu xanh.",
                        "A molded lotus leaf plate for dining, catering, and material-conscious events.",
                        "Lá sen ép định hình", "Molded lotus leaf", ProductCategory.TABLEWARE,
                        ProductSaleMode.COMBO, "4000", "8750", 20, "80000", 20, true, 2,
                        List.of("Bề mặt tự nhiên độc bản", "Phù hợp trình bày món ăn", "Giảm vật dụng nhựa"),
                        List.of("Unique natural surface", "Designed for food presentation", "Reduces plastic tableware"),
                        List.of("Nhà hàng và catering", "Tiệc và sự kiện", "Bộ bàn ăn xanh"),
                        List.of("Restaurants and catering", "Parties and events", "Sustainable table settings"),
                        List.of("Combo 20 dĩa", "Giá combo 80.000 VNĐ"),
                        List.of("Set of 20 plates", "Set price VND 80,000")),
                product(
                        "chen-la-luc-binh", "Chén lá lục bình", "Water hyacinth leaf bowl",
                        "Chén nhẹ cho món ăn, bánh và đồ khô.",
                        "A lightweight bowl for dishes, snacks, and dry food.",
                        "Chén được ép định hình từ lá lục bình, kết hợp sắc độ tự nhiên với hình dáng gọn cho bàn ăn hiện đại.",
                        "A molded water hyacinth leaf bowl combining natural tones with a compact modern dining form.",
                        "Lá lục bình ép định hình", "Molded water hyacinth leaf", ProductCategory.TABLEWARE,
                        ProductSaleMode.COMBO, "2500", "6250", 20, "50000", 20, true, 3,
                        List.of("Nhẹ và dễ bố trí", "Vân lá tự nhiên", "Phù hợp nhiều món"),
                        List.of("Lightweight and easy to arrange", "Natural leaf texture", "Works with many dishes"),
                        List.of("Món khai vị", "Bánh và đồ ăn nhẹ", "Catering xanh"),
                        List.of("Appetizers", "Pastries and snacks", "Sustainable catering"),
                        List.of("Combo 20 chén", "Giá combo 50.000 VNĐ"),
                        List.of("Set of 20 bowls", "Set price VND 50,000")),
                product(
                        "lot-ly-luc-binh", "Lót ly từ lục bình", "Water hyacinth coaster",
                        "Lót ly ép từ sợi và phụ phẩm lục bình.",
                        "A coaster pressed from water hyacinth fibers and by-products.",
                        "Phụ kiện bàn uống nước có bề mặt sợi đặc trưng, phù hợp quán cà phê, văn phòng và quà tặng.",
                        "A fiber-textured tabletop accessory for cafes, offices, and gifting programs.",
                        "Sợi và phụ phẩm lục bình", "Water hyacinth fiber and by-products", ProductCategory.LIFESTYLE,
                        ProductSaleMode.COMBO, "2200", "6250", 10, "22000", 10, false, 4,
                        List.of("Tận dụng phụ phẩm", "Bảo vệ bề mặt bàn", "Dễ phối trong không gian tự nhiên"),
                        List.of("Upcycles by-products", "Protects table surfaces", "Fits natural interiors"),
                        List.of("Quán cà phê", "Văn phòng", "Quà tặng thương hiệu"),
                        List.of("Cafes", "Offices", "Branded gifts"),
                        List.of("Combo 10 lót ly", "Giá combo 22.000 VNĐ"),
                        List.of("Set of 10 coasters", "Set price VND 22,000")),
                product(
                        "tui-dan-bao-ve-trai-cay", "Túi đan bảo vệ trái cây", "Woven fruit protection bag",
                        "Túi đan thủ công giúp bảo vệ trái cây trong thu hoạch và trưng bày.",
                        "A handwoven bag that protects fruit during harvesting and display.",
                        "Cấu trúc đan từ thân lục bình ôm quanh trái cây, tạo một giải pháp bảo vệ có thể tùy chỉnh cho nông hộ và doanh nghiệp.",
                        "A woven water hyacinth structure wraps fruit for a customizable protection solution for farms and businesses.",
                        "Thân lục bình đan thủ công", "Handwoven water hyacinth stems", ProductCategory.PACKAGING,
                        ProductSaleMode.RETAIL_AND_B2B, "15000", "50000", null, null, 1, false, 5,
                        List.of("Đan thủ công", "Thông thoáng quanh trái", "Có thể tùy chỉnh kích thước"),
                        List.of("Handwoven", "Ventilated around the fruit", "Custom sizing available"),
                        List.of("Bảo vệ trái cây", "Trưng bày nông sản", "Đơn hàng doanh nghiệp"),
                        List.of("Fruit protection", "Produce display", "Business orders"),
                        List.of("Bán lẻ hoặc theo đơn B2B", "Liên hệ để tùy chỉnh"),
                        List.of("Retail or B2B ordering", "Contact us for customization"))
        );
    }

    private static Product product(
            String slug, String nameVi, String nameEn, String shortVi, String shortEn,
            String descriptionVi, String descriptionEn, String materialVi, String materialEn,
            ProductCategory category, ProductSaleMode saleMode, String unitPrice, String exportPrice,
            Integer comboQuantity, String comboPrice, Integer minimumOrder, boolean featured, int displayOrder,
            List<String> benefitsVi, List<String> benefitsEn, List<String> applicationsVi,
            List<String> applicationsEn, List<String> specificationsVi, List<String> specificationsEn) {
        return Product.builder()
                .slug(slug).nameVi(nameVi).nameEn(nameEn)
                .shortDescriptionVi(shortVi).shortDescriptionEn(shortEn)
                .descriptionVi(descriptionVi).descriptionEn(descriptionEn)
                .materialVi(materialVi).materialEn(materialEn)
                .category(category).saleMode(saleMode)
                .domesticUnitPrice(new BigDecimal(unitPrice)).exportUnitPrice(new BigDecimal(exportPrice))
                .comboQuantity(comboQuantity)
                .domesticComboPrice(comboPrice != null ? new BigDecimal(comboPrice) : null)
                .minimumOrderQuantity(minimumOrder).featured(featured).active(true).displayOrder(displayOrder)
                .benefitsVi(new ArrayList<>(benefitsVi)).benefitsEn(new ArrayList<>(benefitsEn))
                .applicationsVi(new ArrayList<>(applicationsVi)).applicationsEn(new ArrayList<>(applicationsEn))
                .specificationsVi(new ArrayList<>(specificationsVi)).specificationsEn(new ArrayList<>(specificationsEn))
                .build();
    }

    private record ImageSeed(
            String source,
            String fileName,
            String displayResource,
            String thumbnailResource,
            int sortOrder,
            boolean mainImage,
            ProductImageType imageType,
            String altTextVi,
            String altTextEn,
            boolean seed) {
    }
}
