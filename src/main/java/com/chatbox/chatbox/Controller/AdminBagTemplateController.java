package com.chatbox.chatbox.Controller;

import com.chatbox.chatbox.model.BagTemplate;
import com.chatbox.chatbox.repository.BagTemplateRepository;
import com.chatbox.chatbox.service.CloudinaryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/bag-templates")
public class AdminBagTemplateController {

    private static final String CLOUDINARY_FOLDER = "greenshield/bag-templates";

    private final BagTemplateRepository bagTemplateRepository;
    private final CloudinaryService cloudinaryService;

    public AdminBagTemplateController(BagTemplateRepository bagTemplateRepository, CloudinaryService cloudinaryService) {
        this.bagTemplateRepository = bagTemplateRepository;
        this.cloudinaryService = cloudinaryService;
    }

    @GetMapping
    public List<BagTemplate> list() {
        return bagTemplateRepository.findAll();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BagTemplate> create(
            @RequestParam("name") String name,
            @RequestParam("basePrice") Double basePrice,
            @RequestParam("frontImage") MultipartFile frontImage,
            @RequestParam("frontCustomArea") String frontCustomArea,
            @RequestParam("backImage") MultipartFile backImage,
            @RequestParam("backCustomArea") String backCustomArea,
            @RequestParam(value = "previewConfig", required = false) String previewConfig,
            @RequestParam(value = "active", defaultValue = "true") Boolean active) {
        try {
            String frontUrl = cloudinaryService.uploadImage(frontImage, CLOUDINARY_FOLDER);
            String backUrl = cloudinaryService.uploadImage(backImage, CLOUDINARY_FOLDER);
            BagTemplate t = BagTemplate.builder()
                    .name(name)
                    .basePrice(basePrice)
                    .frontImageUrl(frontUrl)
                    .frontCustomArea(frontCustomArea)
                    .backImageUrl(backUrl)
                    .backCustomArea(backCustomArea)
                    .previewConfig(previewConfig)
                    .active(active != null ? active : true)
                    .build();
            return ResponseEntity.status(HttpStatus.CREATED).body(bagTemplateRepository.save(t));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BagTemplate> update(
            @PathVariable Long id,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "basePrice", required = false) Double basePrice,
            @RequestParam(value = "frontImage", required = false) MultipartFile frontImage,
            @RequestParam(value = "frontCustomArea", required = false) String frontCustomArea,
            @RequestParam(value = "backImage", required = false) MultipartFile backImage,
            @RequestParam(value = "backCustomArea", required = false) String backCustomArea,
            @RequestParam(value = "previewConfig", required = false) String previewConfig,
            @RequestParam(value = "active", required = false) Boolean active) {
        return bagTemplateRepository.findById(id)
                .map(t -> {
                    try {
                        if (name != null) t.setName(name);
                        if (basePrice != null) t.setBasePrice(basePrice);
                        if (frontImage != null && !frontImage.isEmpty()) {
                            t.setFrontImageUrl(cloudinaryService.uploadImage(frontImage, CLOUDINARY_FOLDER));
                        }
                        if (frontCustomArea != null) t.setFrontCustomArea(frontCustomArea);
                        if (backImage != null && !backImage.isEmpty()) {
                            t.setBackImageUrl(cloudinaryService.uploadImage(backImage, CLOUDINARY_FOLDER));
                        }
                        if (backCustomArea != null) t.setBackCustomArea(backCustomArea);
                        if (previewConfig != null) t.setPreviewConfig(previewConfig);
                        if (active != null) t.setActive(active);
                        return ResponseEntity.ok(bagTemplateRepository.save(t));
                    } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).<BagTemplate>build();
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (bagTemplateRepository.existsById(id)) {
            bagTemplateRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
