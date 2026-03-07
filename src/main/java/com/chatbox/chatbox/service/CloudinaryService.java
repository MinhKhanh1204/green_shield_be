package com.chatbox.chatbox.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(@Autowired(required = false) Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public String uploadImage(MultipartFile file) throws IOException {
        return uploadImage(file, "greenshield/textures");
    }

    public String uploadImage(MultipartFile file, String folder) throws IOException {
        if (cloudinary == null) {
            throw new IllegalStateException("Cloudinary is not configured. Set cloudinary.cloud-name, api-key, api-secret in application.yml");
        }
        Map<?, ?> params = ObjectUtils.asMap(
                "folder", folder != null ? folder : "greenshield/textures",
                "use_filename", true,
                "unique_filename", true,
                "overwrite", false
        );
        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), params);
        return (String) result.get("secure_url");
    }
}
