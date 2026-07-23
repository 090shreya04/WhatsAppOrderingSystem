package com.example.whatsappOrdering.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.whatsappOrdering.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;
    private final boolean enabled;

    public CloudinaryService(AppProperties appProperties) {
        String url = appProperties.getCloudinary().getUrl();
        if (url != null && !url.isBlank()) {
            this.cloudinary = new Cloudinary(url);
            this.enabled = true;
        } else {
            this.cloudinary = new Cloudinary();
            this.enabled = false;
            log.warn("CLOUDINARY_URL not set — image upload is disabled");
        }
    }

    /**
     * Uploads a file to Cloudinary and returns the secure HTTPS URL.
     * Returns null if Cloudinary is not configured.
     */
    public String uploadImage(MultipartFile file) throws IOException {
        if (!enabled) {
            log.warn("Image upload skipped — Cloudinary not configured");
            return null;
        }
        Map<?, ?> result = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap("resource_type", "image", "folder", "restaurant_menu"));
        return (String) result.get("secure_url");
    }

    public void deleteImage(String publicId) {
        if (!enabled) return;
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            log.warn("Failed to delete Cloudinary image {}: {}", publicId, e.getMessage());
        }
    }
}
