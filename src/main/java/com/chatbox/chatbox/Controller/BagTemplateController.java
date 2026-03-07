package com.chatbox.chatbox.Controller;

import com.chatbox.chatbox.model.BagTemplate;
import com.chatbox.chatbox.repository.BagTemplateRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bag-templates")
public class BagTemplateController {

    private final BagTemplateRepository bagTemplateRepository;

    public BagTemplateController(BagTemplateRepository bagTemplateRepository) {
        this.bagTemplateRepository = bagTemplateRepository;
    }

    /** Public: list active templates */
    @GetMapping
    public List<BagTemplate> list(@RequestParam(required = false) Boolean active) {
        if (active != null && active) {
            return bagTemplateRepository.findByActiveTrue();
        }
        return bagTemplateRepository.findAll();
    }

    /** Public: get by id */
    @GetMapping("/{id}")
    public ResponseEntity<BagTemplate> getById(@PathVariable Long id) {
        return bagTemplateRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
