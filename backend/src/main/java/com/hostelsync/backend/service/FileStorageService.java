package com.hostelsync.backend.service;

import com.hostelsync.backend.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Stores complaint evidence files on the local filesystem.
 * The storage directory is configurable so it can be changed to
 * cloud/object storage later without touching the complaint logic.
 */
@Service
public class FileStorageService {

    /** Allowed MIME types for uploaded evidence */
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "video/mp4",
            "video/webm",
            "video/quicktime"   // .mov
    );

    /** 50 MB in bytes */
    private final long maxFileSize;

    private final Path storageDir;

    public FileStorageService(@Value("${app.upload.dir}") String uploadDir,
                              @Value("${app.upload.max-file-size-bytes}") long maxFileSize) {
        this.maxFileSize = maxFileSize;
        this.storageDir = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.storageDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + storageDir, e);
        }
    }

    /**
     * Validate and store a single file.
     * Returns the safe stored filename (UUID-based).
     * Never uses the original filename in the filesystem path.
     */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Cannot store an empty file.");
        }

        // Validate size
        if (file.getSize() > maxFileSize) {
            throw new BadRequestException(
                    "File exceeds maximum allowed size of " + (maxFileSize / 1_048_576) + " MB.");
        }

        // Validate MIME type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException(
                    "File type not allowed: " + contentType +
                    ". Allowed types: JPEG, PNG, WEBP, MP4, WEBM, MOV.");
        }

        // Derive a safe extension from the content type
        String extension = mimeToExtension(contentType);

        // Use UUID to guarantee uniqueness and prevent path traversal
        String storedName = UUID.randomUUID() + extension;
        Path destination = storageDir.resolve(storedName);

        try {
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }

        return storedName;
    }

    /**
     * Load a stored file as a Spring Resource for streaming download.
     */
    public Resource load(String storedFileName) {
        // Prevent path traversal: only allow simple filenames (no slashes or dots in path)
        if (storedFileName.contains("..") || storedFileName.contains("/") || storedFileName.contains("\\")) {
            throw new BadRequestException("Invalid file name.");
        }

        Path filePath = storageDir.resolve(storedFileName).normalize();

        // Ensure the resolved path is still inside the storage directory
        if (!filePath.startsWith(storageDir)) {
            throw new BadRequestException("Access denied.");
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BadRequestException("File not found: " + storedFileName);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not read file: " + storedFileName, e);
        }
    }

    /**
     * Delete a stored file (called when a complaint attachment is removed).
     */
    public void delete(String storedFileName) {
        if (storedFileName == null || storedFileName.isBlank()) return;
        try {
            Path filePath = storageDir.resolve(storedFileName).normalize();
            if (filePath.startsWith(storageDir)) {
                Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            // Log but don't fail — the DB record will still be removed
            System.err.println("Warning: could not delete file " + storedFileName + ": " + e.getMessage());
        }
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private String mimeToExtension(String mimeType) {
        return switch (mimeType.toLowerCase()) {
            case "image/jpeg"      -> ".jpg";
            case "image/png"       -> ".png";
            case "image/webp"      -> ".webp";
            case "video/mp4"       -> ".mp4";
            case "video/webm"      -> ".webm";
            case "video/quicktime" -> ".mov";
            default                -> ".bin";
        };
    }
}
