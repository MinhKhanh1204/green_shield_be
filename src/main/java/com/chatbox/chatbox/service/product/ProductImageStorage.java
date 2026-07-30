package com.chatbox.chatbox.service.product;

import java.io.IOException;

public interface ProductImageStorage {
    StoredProductImage store(byte[] content, String originalFilename, String slug) throws IOException;

    StoredProductImage storePrepared(
            byte[] displayContent,
            byte[] thumbnailContent,
            String fileName,
            String slug) throws IOException;

    void delete(String storageKey, String cloudinaryPublicId) throws IOException;
}
