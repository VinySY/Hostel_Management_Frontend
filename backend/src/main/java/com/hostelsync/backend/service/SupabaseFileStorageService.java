package com.hostelsync.backend.service;

import com.hostelsync.backend.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Supabase Storage implementation of {@link FileStorageService} using a private bucket.
 * Active when {@code app.storage.type=supabase}.
 * Service-role credentials remain strictly on the backend and are never exposed to clients.
 */
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "supabase")
public class SupabaseFileStorageService implements FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(SupabaseFileStorageService.class);

    private final String baseUrl;
    private final String serviceRoleKey;
    private final String bucket;
    private final long maxFileSize;
    private final Supplier<HttpClient> httpClientFactory;
    private volatile HttpClient httpClient;

    @Autowired
    public SupabaseFileStorageService(
            @Value("${app.supabase.url:}") String supabaseUrl,
            @Value("${app.supabase.service-role-key:}") String serviceRoleKey,
            @Value("${app.supabase.bucket:hostelsync-files}") String bucket,
            @Value("${app.upload.max-file-size-bytes:52428800}") long maxFileSize) {
        this(supabaseUrl, serviceRoleKey, bucket, maxFileSize,
                () -> HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build(),
                null);
    }

    SupabaseFileStorageService(String supabaseUrl,
                               String serviceRoleKey,
                               String bucket,
                               long maxFileSize,
                               HttpClient httpClient) {
        this(supabaseUrl, serviceRoleKey, bucket, maxFileSize,
                () -> HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build(),
                Objects.requireNonNull(httpClient, "httpClient must not be null"));
    }

    SupabaseFileStorageService(String supabaseUrl,
                               String serviceRoleKey,
                               String bucket,
                               long maxFileSize,
                               Supplier<HttpClient> httpClientFactory) {
        this(supabaseUrl, serviceRoleKey, bucket, maxFileSize,
                Objects.requireNonNull(httpClientFactory, "httpClientFactory must not be null"),
                null);
    }

    private SupabaseFileStorageService(String supabaseUrl,
                                       String serviceRoleKey,
                                       String bucket,
                                       long maxFileSize,
                                       Supplier<HttpClient> httpClientFactory,
                                       HttpClient initialClient) {
        if (supabaseUrl == null || supabaseUrl.isBlank()) {
            throw new IllegalStateException("SUPABASE_URL must be configured when STORAGE_TYPE=supabase");
        }
        if (serviceRoleKey == null || serviceRoleKey.isBlank()) {
            throw new IllegalStateException("SUPABASE_SERVICE_ROLE_KEY must be configured when STORAGE_TYPE=supabase");
        }
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("SUPABASE_BUCKET must be configured when STORAGE_TYPE=supabase");
        }

        this.baseUrl = supabaseUrl.trim().replaceAll("/+$", "");
        this.serviceRoleKey = serviceRoleKey.trim();
        this.bucket = bucket.trim();
        this.maxFileSize = maxFileSize;
        this.httpClientFactory = httpClientFactory;
        this.httpClient = initialClient;
    }

    private HttpClient getHttpClient() {
        HttpClient client = httpClient;
        if (client == null) {
            synchronized (this) {
                client = httpClient;
                if (client == null) {
                    client = Objects.requireNonNull(
                            httpClientFactory.get(),
                            "HttpClient factory must not return null");
                    httpClient = client;
                }
            }
        }
        return client;
    }

    @Override
    public String store(MultipartFile file) {
        String storedName = FileStorageValidation.validateAndGenerateFileName(file, maxFileSize);
        String contentType = FileStorageValidation.normalizeContentType(file.getContentType());

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded file bytes", e);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(objectUri(storedName))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + serviceRoleKey)
                .header("apikey", serviceRoleKey)
                .header("Content-Type", contentType != null ? contentType : "application/octet-stream")
                .header("x-upsert", "false")
                .POST(HttpRequest.BodyPublishers.ofByteArray(bytes))
                .build();

        try {
            HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                logger.error("Supabase upload failed for {} with HTTP status {}", storedName, response.statusCode());
                throw new RuntimeException("Failed to store file in Supabase Storage (HTTP " + response.statusCode() + ").");
            }
            return storedName;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while uploading file to Supabase Storage.", e);
        } catch (IOException e) {
            logger.error("Supabase upload I/O error for {}: {}", storedName, e.getMessage());
            throw new RuntimeException("Failed to communicate with Supabase Storage during upload.", e);
        }
    }

    @Override
    public Resource load(String storedFileName) {
        FileStorageValidation.validateStoredFileName(storedFileName);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(objectUri(storedFileName))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + serviceRoleKey)
                .header("apikey", serviceRoleKey)
                .GET()
                .build();

        try {
            HttpResponse<byte[]> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofByteArray());
            int status = response.statusCode();

            if (status >= 200 && status < 300) {
                return new ByteArrayResource(response.body());
            }

            if (isNotFoundResponse(status, response.body())) {
                throw new ResourceNotFoundException("File not found: " + storedFileName);
            }

            logger.error("Supabase download failed for {} with HTTP status {}", storedFileName, status);
            throw new RuntimeException("Failed to retrieve file from Supabase Storage (HTTP " + status + ").");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while downloading file from Supabase Storage.", e);
        } catch (IOException e) {
            logger.error("Supabase download I/O error for {}: {}", storedFileName, e.getMessage());
            throw new RuntimeException("Failed to communicate with Supabase Storage during download.", e);
        }
    }

    @Override
    public void delete(String storedFileName) {
        if (storedFileName == null || storedFileName.isBlank()) {
            return;
        }
        FileStorageValidation.validateStoredFileName(storedFileName);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(objectUri(storedFileName))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + serviceRoleKey)
                .header("apikey", serviceRoleKey)
                .DELETE()
                .build();

        try {
            HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status >= 400 && status != 404) {
                logger.warn("Supabase delete returned HTTP {} for {}", status, storedFileName);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while deleting file {} from Supabase Storage", storedFileName);
        } catch (IOException e) {
            logger.warn("Warning: could not delete file {} from Supabase Storage: {}", storedFileName, e.getMessage());
        }
    }

    private URI objectUri(String storedFileName) {
        String encodedBucket = URLEncoder.encode(bucket, StandardCharsets.UTF_8).replace("+", "%20");
        String encodedKey = URLEncoder.encode(storedFileName, StandardCharsets.UTF_8).replace("+", "%20");
        return URI.create(baseUrl + "/storage/v1/object/" + encodedBucket + "/" + encodedKey);
    }

    private boolean isNotFoundResponse(int status, byte[] bodyBytes) {
        if (status == 404) {
            return true;
        }
        // Supabase Storage frequently returns HTTP 400 with {"statusCode":"404","error":"not_found",...}
        if (status == 400 && bodyBytes != null && bodyBytes.length > 0) {
            String body = new String(bodyBytes, StandardCharsets.UTF_8).toLowerCase();
            return body.contains("not_found") || body.contains("object not found") || body.contains("\"404\"");
        }
        return false;
    }
}

