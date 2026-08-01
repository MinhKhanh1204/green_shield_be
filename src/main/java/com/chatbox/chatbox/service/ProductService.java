package com.chatbox.chatbox.service;

import com.chatbox.chatbox.dto.*;
import com.chatbox.chatbox.model.*;
import com.chatbox.chatbox.repository.ProductImageRepository;
import com.chatbox.chatbox.repository.ProductRepository;
import com.chatbox.chatbox.service.product.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductService {
    public static final int MAX_IMAGES_PER_PRODUCT = 8;

    private final ProductRepository productRepository;
    private final ProductImageRepository imageRepository;
    private final ProductImageStorage imageStorage;

    public ProductService(
            ProductRepository productRepository,
            ProductImageRepository imageRepository,
            ProductImageStorage imageStorage) {
        this.productRepository = productRepository;
        this.imageRepository = imageRepository;
        this.imageStorage = imageStorage;
    }

    @Transactional(readOnly = true)
    public List<ProductCardResponse> listPublic(
            ProductCategory category,
            Boolean featured,
            String search) {
        String normalizedSearch = normalizeSearch(search);
        return productRepository.findAllByActiveTrueOrderByDisplayOrderAscCreatedAtDesc().stream()
                .filter(product -> category == null || product.getCategory() == category)
                .filter(product -> featured == null || product.isFeatured() == featured)
                .filter(product -> matchesSearch(product, normalizedSearch))
                .map(this::toCard)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getPublicBySlug(String slug) {
        Product product = productRepository.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> new NoSuchElementException("Product not found"));
        List<ProductCardResponse> related = productRepository
                .findTop4ByActiveTrueAndCategoryAndIdNotOrderByFeaturedDescDisplayOrderAsc(
                        product.getCategory(), product.getId())
                .stream()
                .map(this::toCard)
                .toList();
        return toDetail(product, related);
    }

    @Transactional(readOnly = true)
    public List<ProductDetailResponse> listAdmin() {
        return productRepository.findAllByOrderByDisplayOrderAscCreatedAtDesc().stream()
                .map(product -> toDetail(product, List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getAdmin(String id) {
        return toDetail(findProduct(id), List.of());
    }

    @Transactional
    public ProductDetailResponse create(ProductUpsertRequest request) {
        validateRequest(request);
        if (productRepository.existsBySlug(request.slug())) {
            throw new IllegalArgumentException("Product slug already exists");
        }
        Product product = new Product();
        applyRequest(product, request);
        return toDetail(productRepository.save(product), List.of());
    }

    @Transactional
    public ProductDetailResponse update(String id, ProductUpsertRequest request) {
        validateRequest(request);
        Product product = findProduct(id);
        if (productRepository.existsBySlugAndIdNot(request.slug(), id)) {
            throw new IllegalArgumentException("Product slug already exists");
        }
        applyRequest(product, request);
        return toDetail(productRepository.save(product), List.of());
    }

    @Transactional
    public void delete(String id) throws IOException {
        Product product = findProduct(id);
        for (ProductImage image : List.copyOf(product.getImages())) {
            imageStorage.delete(image.getStorageKey(), image.getCloudinaryPublicId());
        }
        productRepository.delete(product);
    }

    @Transactional
    public ProductDetailResponse updateStatus(String id, boolean active) {
        Product product = findProduct(id);
        product.setActive(active);
        return toDetail(productRepository.save(product), List.of());
    }

    @Transactional
    public ProductDetailResponse updateFeatured(String id, boolean featured) {
        Product product = findProduct(id);
        product.setFeatured(featured);
        return toDetail(productRepository.save(product), List.of());
    }

    @Transactional
    public ProductDetailResponse updateDisplayOrder(String id, int displayOrder) {
        if (displayOrder < 0) throw new IllegalArgumentException("Display order must be non-negative");
        Product product = findProduct(id);
        product.setDisplayOrder(displayOrder);
        return toDetail(productRepository.save(product), List.of());
    }

    @Transactional
    public List<ProductImageResponse> uploadImages(
            String productId,
            List<MultipartFile> files,
            String altTextVi,
            String altTextEn,
            ProductImageType imageType) throws IOException {
        Product product = findProduct(productId);
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("At least one image is required");
        }
        long existingCount = imageRepository.countByProductId(productId);
        if (existingCount + files.size() > MAX_IMAGES_PER_PRODUCT) {
            throw new IllegalArgumentException("A product can have at most 8 images");
        }

        int nextOrder = product.getImages().stream().mapToInt(ProductImage::getSortOrder).max().orElse(-1) + 1;
        boolean hasMain = product.getImages().stream().anyMatch(ProductImage::isMainImage);
        List<ProductImage> created = new ArrayList<>();

        try {
            for (int index = 0; index < files.size(); index++) {
                ProductImageFileValidator.ValidatedImage validated = ProductImageFileValidator.validate(files.get(index));
                StoredProductImage stored = imageStorage.store(
                        validated.content(), validated.originalFilename(), product.getSlug());
                ProductImage image = ProductImage.builder()
                        .imageUrl(stored.imageUrl())
                        .thumbnailUrl(stored.thumbnailUrl())
                        .cloudinaryPublicId(stored.cloudinaryPublicId())
                        .storageKey(stored.storageKey())
                        .altTextVi(resolveAlt(altTextVi, product.getNameVi(), nextOrder + index + 1))
                        .altTextEn(resolveAlt(altTextEn, product.getNameEn(), nextOrder + index + 1))
                        .imageType(imageType != null ? imageType : ProductImageType.GALLERY)
                        .sortOrder(nextOrder + index)
                        .mainImage(!hasMain && index == 0)
                        .build();
                product.addImage(image);
                created.add(image);
            }
            productRepository.save(product);
            return created.stream().map(this::toImage).toList();
        } catch (Exception exception) {
            for (ProductImage image : created) {
                try {
                    imageStorage.delete(image.getStorageKey(), image.getCloudinaryPublicId());
                } catch (Exception ignored) {
                    // Preserve the original upload failure while best-effort cleaning stored files.
                }
            }
            throw exception;
        }
    }

    @Transactional
    public ProductImageResponse updateImage(
            String productId,
            Long imageId,
            ProductImageUpdateRequest request) {
        ProductImage image = findOwnedImage(productId, imageId);
        image.setAltTextVi(request.altTextVi().trim());
        image.setAltTextEn(request.altTextEn().trim());
        image.setImageType(request.imageType());
        return toImage(imageRepository.save(image));
    }

    @Transactional
    public void deleteImage(String productId, Long imageId) throws IOException {
        Product product = findProduct(productId);
        ProductImage image = findOwnedImage(productId, imageId);
        boolean wasMain = image.isMainImage();
        imageStorage.delete(image.getStorageKey(), image.getCloudinaryPublicId());
        product.removeImage(image);
        imageRepository.delete(image);
        if (wasMain && !product.getImages().isEmpty()) {
            product.getImages().stream()
                    .min(Comparator.comparingInt(ProductImage::getSortOrder))
                    .ifPresent(candidate -> candidate.setMainImage(true));
        }
        normalizeImageOrder(product);
        productRepository.save(product);
    }

    @Transactional
    public List<ProductImageResponse> reorderImages(String productId, List<Long> imageIds) {
        Product product = findProduct(productId);
        Set<Long> currentIds = product.getImages().stream().map(ProductImage::getId).collect(Collectors.toSet());
        if (imageIds == null || imageIds.size() != currentIds.size() || !currentIds.equals(new HashSet<>(imageIds))) {
            throw new IllegalArgumentException("Image reorder list must contain every product image exactly once");
        }
        Map<Long, ProductImage> byId = product.getImages().stream()
                .collect(Collectors.toMap(ProductImage::getId, image -> image));
        for (int index = 0; index < imageIds.size(); index++) {
            byId.get(imageIds.get(index)).setSortOrder(index);
        }
        productRepository.save(product);
        return product.getImages().stream()
                .sorted(Comparator.comparingInt(ProductImage::getSortOrder))
                .map(this::toImage)
                .toList();
    }

    @Transactional
    public ProductImageResponse setMainImage(String productId, Long imageId) {
        Product product = findProduct(productId);
        ProductImage selected = findOwnedImage(productId, imageId);
        product.getImages().forEach(image -> image.setMainImage(Objects.equals(image.getId(), imageId)));
        productRepository.save(product);
        return toImage(selected);
    }

    @Transactional
    public ProductImage addPreparedImage(
            Product product,
            byte[] display,
            byte[] thumbnail,
            String fileName,
            String altTextVi,
            String altTextEn,
            ProductImageType imageType,
            int sortOrder,
            boolean mainImage) throws IOException {
        StoredProductImage stored = imageStorage.storePrepared(display, thumbnail, fileName, product.getSlug());
        if (mainImage) product.getImages().forEach(image -> image.setMainImage(false));
        ProductImage image = ProductImage.builder()
                .imageUrl(stored.imageUrl())
                .thumbnailUrl(stored.thumbnailUrl())
                .cloudinaryPublicId(stored.cloudinaryPublicId())
                .storageKey(stored.storageKey())
                .altTextVi(altTextVi)
                .altTextEn(altTextEn)
                .imageType(imageType)
                .sortOrder(sortOrder)
                .mainImage(mainImage)
                .build();
        product.addImage(image);
        return image;
    }

    @Transactional
    public Product saveSeedProduct(Product product) {
        validateProduct(product);
        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public Optional<Product> findBySlugForSeed(String slug) {
        return productRepository.findBySlug(slug);
    }

    private Product findProduct(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Product not found"));
    }

    private ProductImage findOwnedImage(String productId, Long imageId) {
        ProductImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new NoSuchElementException("Product image not found"));
        if (!Objects.equals(image.getProduct().getId(), productId)) {
            throw new NoSuchElementException("Product image not found");
        }
        return image;
    }

    private void validateRequest(ProductUpsertRequest request) {
        if (request.saleMode().includesCombo()
                && (request.comboQuantity() == null || request.domesticComboPrice() == null)) {
            throw new IllegalArgumentException("Combo quantity and combo price are required for combo products");
        }
    }

    private void validateProduct(Product product) {
        if (product.getSaleMode().includesCombo()
                && (product.getComboQuantity() == null || product.getDomesticComboPrice() == null)) {
            throw new IllegalArgumentException("Combo quantity and combo price are required for combo products");
        }
    }

    private void applyRequest(Product product, ProductUpsertRequest request) {
        product.setSlug(request.slug().trim());
        product.setNameVi(request.nameVi().trim());
        product.setNameEn(request.nameEn().trim());
        product.setShortDescriptionVi(request.shortDescriptionVi().trim());
        product.setShortDescriptionEn(request.shortDescriptionEn().trim());
        product.setDescriptionVi(request.descriptionVi().trim());
        product.setDescriptionEn(request.descriptionEn().trim());
        product.setMaterialVi(request.materialVi().trim());
        product.setMaterialEn(request.materialEn().trim());
        product.setCategory(request.category());
        product.setSaleMode(request.saleMode());
        product.setDomesticUnitPrice(request.domesticUnitPrice());
        product.setExportUnitPrice(request.exportUnitPrice());
        product.setComboQuantity(request.saleMode().includesCombo() ? request.comboQuantity() : null);
        product.setDomesticComboPrice(request.saleMode().includesCombo() ? request.domesticComboPrice() : null);
        product.setMinimumOrderQuantity(request.minimumOrderQuantity());
        product.setFeatured(request.featured());
        product.setActive(request.active());
        product.setDisplayOrder(request.displayOrder());
        product.setBenefitsVi(copy(request.benefitsVi()));
        product.setBenefitsEn(copy(request.benefitsEn()));
        product.setApplicationsVi(copy(request.applicationsVi()));
        product.setApplicationsEn(copy(request.applicationsEn()));
        product.setSpecificationsVi(copy(request.specificationsVi()));
        product.setSpecificationsEn(copy(request.specificationsEn()));
    }

    private ProductCardResponse toCard(Product product) {
        ProductImageResponse mainImage = product.getImages().stream()
                .filter(ProductImage::isMainImage)
                .findFirst()
                .or(() -> product.getImages().stream().findFirst())
                .map(this::toImage)
                .orElse(null);
        return new ProductCardResponse(
                product.getId(), product.getSlug(), product.getNameVi(), product.getNameEn(),
                product.getShortDescriptionVi(), product.getShortDescriptionEn(),
                product.getMaterialVi(), product.getMaterialEn(), product.getCategory(), product.getSaleMode(),
                product.getDomesticUnitPrice(), product.getExportUnitPrice(), product.getComboQuantity(),
                product.getDomesticComboPrice(), product.isFeatured(), product.isActive(),
                product.getDisplayOrder(), mainImage);
    }

    private ProductDetailResponse toDetail(Product product, List<ProductCardResponse> related) {
        List<ProductImageResponse> images = product.getImages().stream()
                .sorted(Comparator.comparingInt(ProductImage::getSortOrder))
                .map(this::toImage)
                .toList();
        return new ProductDetailResponse(
                product.getId(), product.getSlug(), product.getNameVi(), product.getNameEn(),
                product.getShortDescriptionVi(), product.getShortDescriptionEn(),
                product.getDescriptionVi(), product.getDescriptionEn(),
                product.getMaterialVi(), product.getMaterialEn(), product.getCategory(), product.getSaleMode(),
                product.getDomesticUnitPrice(), product.getExportUnitPrice(), product.getComboQuantity(),
                product.getDomesticComboPrice(), product.getMinimumOrderQuantity(), product.isFeatured(),
                product.isActive(), product.getDisplayOrder(), List.copyOf(product.getBenefitsVi()),
                List.copyOf(product.getBenefitsEn()), List.copyOf(product.getApplicationsVi()),
                List.copyOf(product.getApplicationsEn()), List.copyOf(product.getSpecificationsVi()),
                List.copyOf(product.getSpecificationsEn()), images, related,
                product.getCreatedAt(), product.getUpdatedAt());
    }

    private ProductImageResponse toImage(ProductImage image) {
        return new ProductImageResponse(
                image.getId(), image.getImageUrl(), image.getThumbnailUrl(), image.getAltTextVi(),
                image.getAltTextEn(), image.getImageType(), image.getSortOrder(), image.isMainImage(),
                image.getCreatedAt());
    }

    private static boolean matchesSearch(Product product, String search) {
        if (search == null) return true;
        return String.join(" ", product.getNameVi(), product.getNameEn(), product.getMaterialVi(),
                        product.getMaterialEn(), product.getShortDescriptionVi(), product.getShortDescriptionEn())
                .toLowerCase(Locale.ROOT)
                .contains(search);
    }

    private static String normalizeSearch(String search) {
        return search == null || search.isBlank() ? null : search.trim().toLowerCase(Locale.ROOT);
    }

    private static List<String> copy(List<String> values) {
        if (values == null) return new ArrayList<>();
        return values.stream().map(String::trim).filter(value -> !value.isBlank())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static String resolveAlt(String requested, String productName, int position) {
        return requested == null || requested.isBlank()
                ? productName + " - image " + position
                : requested.trim();
    }

    private static void normalizeImageOrder(Product product) {
        List<ProductImage> sorted = product.getImages().stream()
                .sorted(Comparator.comparingInt(ProductImage::getSortOrder))
                .toList();
        for (int index = 0; index < sorted.size(); index++) sorted.get(index).setSortOrder(index);
    }
}
