package com.chatbox.chatbox.Controller;

import com.chatbox.chatbox.dto.ProductCardResponse;
import com.chatbox.chatbox.dto.ProductDetailResponse;
import com.chatbox.chatbox.model.ProductCategory;
import com.chatbox.chatbox.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductCardResponse> list(
            @RequestParam(required = false) ProductCategory category,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search) {
        if (Boolean.FALSE.equals(active)) return List.of();
        return productService.listPublic(category, featured, search);
    }

    @GetMapping("/{slug}")
    public ProductDetailResponse detail(@PathVariable String slug) {
        return productService.getPublicBySlug(slug);
    }
}
