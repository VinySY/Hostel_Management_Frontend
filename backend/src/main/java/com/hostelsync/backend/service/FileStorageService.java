package com.hostelsync.backend.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Abstraction for storing, retrieving, and deleting uploaded evidence and profile files.
 * Implementations are selected via {@code app.storage.type} ({@code local} or {@code supabase}).
 */
public interface FileStorageService {

    /**
     * Validate and store a single file, returning its safe UUID-based stored filename.
     */
    String store(MultipartFile file);

    /**
     * Load a stored file as a Spring Resource for streaming download.
     */
    Resource load(String storedFileName);

    /**
     * Delete a stored file by its stored filename.
     */
    void delete(String storedFileName);
}
