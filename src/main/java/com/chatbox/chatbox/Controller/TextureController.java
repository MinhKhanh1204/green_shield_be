package com.chatbox.chatbox.Controller;

import com.chatbox.chatbox.model.Texture;
import com.chatbox.chatbox.repository.TextureRepository;
import com.chatbox.chatbox.service.CloudinaryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
public class TextureController {

    private final TextureRepository textureRepository;
    private final CloudinaryService cloudinaryService;

    public TextureController(TextureRepository textureRepository, CloudinaryService cloudinaryService) {
        this.textureRepository = textureRepository;
        this.cloudinaryService = cloudinaryService;
    }

    /** Public: get all textures with optional search by name */
    @GetMapping("/api/v1/textures")
    public List<Texture> getAll(@RequestParam(required = false) String search) {
        if (search != null && !search.isBlank()) {
            return textureRepository.findByNameContainingIgnoreCase(search.trim());
        }
        return textureRepository.findAll();
    }

    /** Admin: create texture (image required, name optional) */
    @PostMapping(value = "/api/v1/admin/textures", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Texture> create(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "name", required = false) String name) {
        try {
            String imageUrl = cloudinaryService.uploadImage(image);
            Texture texture = Texture.builder()
                    .imageUrl(imageUrl)
                    .name(name != null && !name.isBlank() ? name.trim() : null)
                    .build();
            return ResponseEntity.status(HttpStatus.CREATED).body(textureRepository.save(texture));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /** Admin: update texture (chỉ tên) */
    @PutMapping(value = "/api/v1/admin/textures/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Texture> update(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return textureRepository.findById(id)
                .map(t -> {
                    String name = body.get("name");
                    if (name != null) {
                        t.setName(name.isBlank() ? null : name.trim());
                    }
                    return ResponseEntity.ok(textureRepository.save(t));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** Admin: delete texture */
    @DeleteMapping("/api/v1/admin/textures/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (textureRepository.existsById(id)) {
            textureRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
