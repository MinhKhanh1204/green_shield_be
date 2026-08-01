package com.chatbox.chatbox.Controller;

import com.chatbox.chatbox.dto.*;
import com.chatbox.chatbox.model.ProductImageType;
import com.chatbox.chatbox.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/products")
public class AdminProductController {
    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductDetailResponse> list() {
        return productService.listAdmin();
    }

    @GetMapping("/{id}")
    public ProductDetailResponse detail(@PathVariable String id) {
        return productService.getAdmin(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDetailResponse create(@Valid @RequestBody ProductUpsertRequest request) {
        return productService.create(request);
    }

    @PutMapping("/{id}")
    public ProductDetailResponse update(
            @PathVariable String id,
            @Valid @RequestBody ProductUpsertRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) throws IOException {
        productService.delete(id);
    }

    @PatchMapping("/{id}/status")
    public ProductDetailResponse status(
            @PathVariable String id,
            @RequestBody ProductPatchRequests.Status request) {
        return productService.updateStatus(id, request.active());
    }

    @PatchMapping("/{id}/featured")
    public ProductDetailResponse featured(
            @PathVariable String id,
            @RequestBody ProductPatchRequests.Featured request) {
        return productService.updateFeatured(id, request.featured());
    }

    @PatchMapping("/{id}/display-order")
    public ProductDetailResponse displayOrder(
            @PathVariable String id,
            @Valid @RequestBody ProductPatchRequests.DisplayOrder request) {
        return productService.updateDisplayOrder(id, request.displayOrder());
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public List<ProductImageResponse> uploadImages(
            @PathVariable String id,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(required = false) String altTextVi,
            @RequestParam(required = false) String altTextEn,
            @RequestParam(required = false, defaultValue = "GALLERY") ProductImageType imageType) throws IOException {
        return productService.uploadImages(id, files, altTextVi, altTextEn, imageType);
    }

    @PutMapping("/{id}/images/{imageId}")
    public ProductImageResponse updateImage(
            @PathVariable String id,
            @PathVariable Long imageId,
            @Valid @RequestBody ProductImageUpdateRequest request) {
        return productService.updateImage(id, imageId, request);
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteImage(@PathVariable String id, @PathVariable Long imageId) throws IOException {
        productService.deleteImage(id, imageId);
    }

    @PatchMapping("/{id}/images/reorder")
    public List<ProductImageResponse> reorderImages(
            @PathVariable String id,
            @Valid @RequestBody ProductImageReorderRequest request) {
        return productService.reorderImages(id, request.imageIds());
    }

    @PatchMapping("/{id}/images/{imageId}/main")
    public ProductImageResponse setMainImage(@PathVariable String id, @PathVariable Long imageId) {
        return productService.setMainImage(id, imageId);
    }
}
