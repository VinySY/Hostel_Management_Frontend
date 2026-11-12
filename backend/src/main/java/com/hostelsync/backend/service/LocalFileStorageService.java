package com.hostelsync.backend.service;

import com.hostelsync.backend.exception.BadRequestException;
import com.hostelsync.backend.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

/**
 * Local filesystem implementation of {@link FileStorageService} for development.
 * Active when {@code app.storage.type=local} (or unset).
 */
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private final long maxFileSize;
    private final Path storageDir;

    public LocalFileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir,
                                   @Value("${app.upload.max-file-size-bytes:52428800}") long maxFileSize) {
        this.maxFileSize = maxFileSize;
        this.storageDir = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.storageDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + storageDir, e);
        }
    }

    @Override
    public String store(MultipartFile file) {
        String storedName = FileStorageValidation.validateAndGenerateFileName(file, maxFileSize);
        Path destination = storageDir.resolve(storedName).normalize();
        if (!destination.startsWith(storageDir)) {
            throw new BadRequestException("Invalid storage path.");
        }

        try (var inputStream = file.getInputStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file locally: " + e.getMessage(), e);
        }

        return storedName;
    }

    @Override
    public Resource load(String storedFileName) {
        FileStorageValidation.validateStoredFileName(storedFileName);

        Path filePath = storageDir.resolve(storedFileName).normalize();
        if (!filePath.startsWith(storageDir)) {
            throw new BadRequestException("Access denied.");
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("File not found: " + storedFileName);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not read file: " + storedFileName, e);
        }
    }

    @Override
    public void delete(String storedFileName) {
        if (storedFileName == null || storedFileName.isBlank()) {
            return;
        }
        FileStorageValidation.validateStoredFileName(storedFileName);
        try {
            Path filePath = storageDir.resolve(storedFileName).normalize();
            if (filePath.startsWith(storageDir)) {
                Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            System.err.println("Warning: could not delete local file " + storedFileName + ": " + e.getMessage());
        }
    }
}
