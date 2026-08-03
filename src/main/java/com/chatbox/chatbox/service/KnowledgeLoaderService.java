package com.chatbox.chatbox.service;

import com.chatbox.chatbox.model.Product;
import com.chatbox.chatbox.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class KnowledgeLoaderService {

    @Autowired
    private ProductRepository productRepository;

    public String loadKnowledge() {
        StringBuilder knowledgeBuilder = new StringBuilder();

        // Đọc nội dung Markdown từ classpath để hoạt động cả khi đóng gói thành JAR.
        try (var inputStream = new ClassPathResource("templates/greenshield_resource.md").getInputStream()) {
            String fileContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            knowledgeBuilder.append("=== COMPANY PROFILE ===\n").append(fileContent).append("\n\n");
        } catch (IOException e) {
            knowledgeBuilder.append("⚠️ Could not read knowledge file.\n");
        }

//        // Đọc dữ liệu sản phẩm từ database
//        List<Product> products = productRepository.findAll();
//
//        knowledgeBuilder.append("=== PRODUCT CATALOG ===\n");
//        for (Product p : products) {
//            knowledgeBuilder
//                    .append("Product: ").append(p.getName()).append("\n")
//                    .append("Category: ").append(p.getCategory()).append("\n")
//                    .append("Material: ").append(p.getMaterial()).append("\n")
//                    .append("Price: ").append(p.getPrice()).append("\n")
//                    .append("Description: ").append(p.getDescription()).append("\n\n");
//        }

        return knowledgeBuilder.toString();
    }
}
