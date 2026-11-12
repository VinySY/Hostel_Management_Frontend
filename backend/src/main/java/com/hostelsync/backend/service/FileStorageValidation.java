package com.hostelsync.backend.service;

import com.hostelsync.backend.exception.BadRequestException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Shared validation and safe filename utilities for all FileStorageService implementations.
 */
public final class FileStorageValidation {

    public static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "video/mp4",
            "video/webm",
            "video/quicktime"
    );

    private FileStorageValidation() {}

    /**
     * Validates size and MIME type of an incoming multipart file and returns a UUID-based safe filename.
     */
    public static String validateAndGenerateFileName(MultipartFile file, long maxFileSize) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Cannot store an empty file.");
        }

        if (file.getSize() > maxFileSize) {
            throw new BadRequestException(
                    "File exceeds maximum allowed size of " + (maxFileSize / 1_048_576) + " MB.");
        }

        String normalizedType = normalizeContentType(file.getContentType());
        if (normalizedType == null || !ALLOWED_TYPES.contains(normalizedType)) {
            throw new BadRequestException(
                    "File type not allowed: " + file.getContentType() +
                    ". Allowed types: JPEG, PNG, WEBP, MP4, WEBM, MOV.");
        }

        return UUID.randomUUID() + mimeToExtension(normalizedType);
    }

    /**
     * Normalizes a Content-Type header value by stripping optional parameters (e.g. charset).
     */
    public static String normalizeContentType(String contentType) {
        if (contentType == null) {
            return null;
        }
        String base = contentType.toLowerCase(Locale.ROOT).split(";")[0].trim();
        return base.isEmpty() ? null : base;
    }

    /**
     * Validates a stored filename to prevent path traversal and invalid object keys.
     */
    public static void validateStoredFileName(String storedFileName) {
        if (storedFileName == null || storedFileName.isBlank()) {
            throw new BadRequestException("Invalid file name.");
        }
        if (storedFileName.contains("..")
                || storedFileName.contains("/")
                || storedFileName.contains("\\")
                || storedFileName.contains("?")
                || storedFileName.contains("#")
                || storedFileName.chars().anyMatch(Character::isWhitespace)) {
            throw new BadRequestException("Invalid file name.");
        }
    }

    public static String mimeToExtension(String mimeType) {
        return switch (mimeType.toLowerCase(Locale.ROOT)) {
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

