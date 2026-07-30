package com.chatbox.chatbox.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
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
        ensureConfigured();
        Map<?, ?> params = ObjectUtils.asMap(
                "folder", folder != null ? folder : "greenshield/textures",
                "use_filename", true,
                "unique_filename", true,
                "overwrite", false
        );
        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), params);
        return (String) result.get("secure_url");
    }

    public Map<String, String> uploadProductImage(byte[] bytes, String fileName, String folder) throws IOException {
        ensureConfigured();
        Map<?, ?> result = cloudinary.uploader().upload(bytes, ObjectUtils.asMap(
                "folder", folder,
                "public_id", fileName,
                "overwrite", false,
                "resource_type", "image"
        ));
        Map<String, String> output = new HashMap<>();
        output.put("secureUrl", String.valueOf(result.get("secure_url")));
        output.put("publicId", String.valueOf(result.get("public_id")));
        return Collections.unmodifiableMap(output);
    }

    public void deleteImage(String publicId) throws IOException {
        if (publicId == null || publicId.isBlank()) return;
        ensureConfigured();
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }

    /**
     * Upload an audio file to Cloudinary.
     * Note: Cloudinary requires resource_type=video for many audio formats (mp3/wav/m4a).
     */
    public Map<String, Object> uploadAudio(MultipartFile file, String folder) throws IOException {
        ensureConfigured();
        Map<String, Object> params = new HashMap<>();
        params.put("resource_type", "video");
        params.put("folder", folder != null ? folder : "greenshield/audio");
        params.put("use_filename", true);
        params.put("unique_filename", true);
        params.put("overwrite", false);

        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), params);
        Map<String, Object> out = new HashMap<>();
        out.put("secure_url", result.get("secure_url"));
        out.put("public_id", result.get("public_id"));
        out.put("bytes", result.get("bytes"));
        out.put("format", result.get("format"));
        out.put("resource_type", result.get("resource_type"));
        out.put("original_filename", result.get("original_filename"));
        return out;
    }

    private void ensureConfigured() {
        if (cloudinary == null) {
            throw new IllegalStateException("Cloudinary is not configured. Set cloudinary.cloud-name, api-key, api-secret in application.yml");
        }
    }
}
