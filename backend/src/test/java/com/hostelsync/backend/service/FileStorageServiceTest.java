package com.hostelsync.backend.service;

import com.hostelsync.backend.exception.BadRequestException;
import com.hostelsync.backend.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit and configuration tests for {@link FileStorageService} implementations:
 * local filesystem storage, Supabase storage (mocked HTTP), validation, and STORAGE_TYPE selection.
 */
class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void localStoreLoadAndDeleteRoundTrip() throws IOException {
        LocalFileStorageService service = new LocalFileStorageService(tempDir.toString(), 52_428_800L);
        byte[] content = "webp-binary-data".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file", "evidence.webp", "image/webp", content);

        String storedName = service.store(file);
        assertTrue(storedName.endsWith(".webp"));
        assertTrue(Files.exists(tempDir.resolve(storedName)));

        Resource loaded = service.load(storedName);
        try (var in = loaded.getInputStream()) {
            assertArrayEquals(content, in.readAllBytes());
        }

        service.delete(storedName);
        assertFalse(Files.exists(tempDir.resolve(storedName)));
    }

    @Test
    void localLoadMissingFileThrowsNotFound() {
        LocalFileStorageService service = new LocalFileStorageService(tempDir.toString(), 52_428_800L);
        assertThrows(ResourceNotFoundException.class, () -> service.load("missing-file.webp"));
    }

    @Test
    void localRejectsPathTraversalDisallowedMimeAndOversize() {
        LocalFileStorageService service = new LocalFileStorageService(tempDir.toString(), 100L);

        assertThrows(BadRequestException.class, () -> service.load("../secret.txt"));
        assertThrows(BadRequestException.class, () -> service.load("sub/file.png"));

        MockMultipartFile badMime = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", new byte[]{1, 2, 3});
        assertThrows(BadRequestException.class, () -> service.store(badMime));

        MockMultipartFile tooBig = new MockMultipartFile(
                "file", "big.png", "image/png", new byte[101]);
        assertThrows(BadRequestException.class, () -> service.store(tooBig));
    }

    @Test
    @SuppressWarnings("unchecked")
    void supabaseStoreLoadDeleteConstructsAuthorizedRequestsAndHandlesErrors() throws Exception {
        HttpClient mockClient = mock(HttpClient.class);
        SupabaseFileStorageService service = new SupabaseFileStorageService(
                "https://example.supabase.co",
                "test-service-role-key",
                "hostelsync-files",
                52_428_800L,
                mockClient);

        HttpResponse<String> uploadOk = mock(HttpResponse.class);
        when(uploadOk.statusCode()).thenReturn(200);
        when(mockClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(uploadOk);

        MockMultipartFile photo = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[]{10, 20, 30});
        String storedName = service.store(photo);
        assertTrue(storedName.endsWith(".jpg"));

        verify(mockClient).send(argThat(req ->
                req.method().equals("POST")
                        && req.uri().toString().equals("https://example.supabase.co/storage/v1/object/hostelsync-files/" + storedName)
                        && req.headers().firstValue("Authorization").orElse("").equals("Bearer test-service-role-key")
                        && req.headers().firstValue("apikey").orElse("").equals("test-service-role-key")
                        && req.headers().firstValue("Content-Type").orElse("").equals("image/jpeg")
        ), eq(HttpResponse.BodyHandlers.ofString()));

        HttpResponse<byte[]> downloadOk = mock(HttpResponse.class);
        when(downloadOk.statusCode()).thenReturn(200);
        when(downloadOk.body()).thenReturn(new byte[]{10, 20, 30});
        when(mockClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofByteArray())))
                .thenReturn(downloadOk);

        Resource downloaded = service.load(storedName);
        assertArrayEquals(new byte[]{10, 20, 30}, downloaded.getInputStream().readAllBytes());

        HttpResponse<byte[]> notFound = mock(HttpResponse.class);
        when(notFound.statusCode()).thenReturn(400);
        when(notFound.body()).thenReturn("{\"statusCode\":\"404\",\"error\":\"not_found\",\"message\":\"Object not found\"}".getBytes(StandardCharsets.UTF_8));
        when(mockClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofByteArray())))
                .thenReturn(notFound);
        assertThrows(ResourceNotFoundException.class, () -> service.load("missing.jpg"));

        HttpResponse<byte[]> serverError = mock(HttpResponse.class);
        when(serverError.statusCode()).thenReturn(502);
        when(serverError.body()).thenReturn(new byte[0]);
        when(mockClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofByteArray())))
                .thenReturn(serverError);
        assertThrows(RuntimeException.class, () -> service.load("error.jpg"));

        HttpResponse<String> deleteOk = mock(HttpResponse.class);
        when(deleteOk.statusCode()).thenReturn(200);
        when(mockClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(deleteOk);
        service.delete(storedName);
        verify(mockClient).send(argThat(req ->
                req.method().equals("DELETE")
                        && req.uri().toString().equals("https://example.supabase.co/storage/v1/object/hostelsync-files/" + storedName)
        ), eq(HttpResponse.BodyHandlers.ofString()));
    }

    @Test
    void supabaseRequiresCredentialsAndFailsFastOnUploadError() throws Exception {
        assertThrows(IllegalStateException.class, () ->
                new SupabaseFileStorageService("", "key", "hostelsync-files", 1024L));
        assertThrows(IllegalStateException.class, () ->
                new SupabaseFileStorageService("https://example.supabase.co", "", "hostelsync-files", 1024L));
        assertThrows(IllegalStateException.class, () ->
                new SupabaseFileStorageService("https://example.supabase.co", "key", "", 1024L));

        HttpClient mockClient = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> failedUpload = mock(HttpResponse.class);
        when(failedUpload.statusCode()).thenReturn(500);
        when(mockClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(failedUpload);

        SupabaseFileStorageService service = new SupabaseFileStorageService(
                "https://example.supabase.co", "key", "hostelsync-files", 1024L, mockClient);
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2});
        assertThrows(RuntimeException.class, () -> service.store(file));
    }

    @Test
    void storageImplementationSelectedByStorageTypeProperty() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withUserConfiguration(LocalFileStorageService.class, SupabaseFileStorageService.class);

        runner.withPropertyValues(
                "app.storage.type=local",
                "app.upload.dir=" + tempDir.toString()
        ).run(ctx -> {
            assertEquals(1, ctx.getBeansOfType(FileStorageService.class).size());
            assertTrue(ctx.getBean(FileStorageService.class) instanceof LocalFileStorageService);
        });

        runner.withPropertyValues(
                "app.storage.type=supabase",
                "app.supabase.url=https://example.supabase.co",
                "app.supabase.service-role-key=dummy-test-key",
                "app.supabase.bucket=hostelsync-files"
        ).run(ctx -> {
            assertEquals(1, ctx.getBeansOfType(FileStorageService.class).size());
            assertTrue(ctx.getBean(FileStorageService.class) instanceof SupabaseFileStorageService);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void supabaseInitializesHttpClientLazilyOnFirstStorageOperation() throws Exception {
        AtomicInteger factoryInvocations = new AtomicInteger(0);
        HttpClient mockClient = mock(HttpClient.class);

        SupabaseFileStorageService service = new SupabaseFileStorageService(
                "https://example.supabase.co",
                "test-service-role-key",
                "hostelsync-files",
                1024L,
                () -> {
                    factoryInvocations.incrementAndGet();
                    return mockClient;
                });

        assertEquals(0, factoryInvocations.get(), "HttpClient must not be created during construction");

        service.delete("");
        assertEquals(0, factoryInvocations.get(), "Blank delete should return before creating HttpClient");

        HttpResponse<String> okStringResponse = mock(HttpResponse.class);
        when(okStringResponse.statusCode()).thenReturn(200);
        when(mockClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(okStringResponse);

        HttpResponse<byte[]> okBytesResponse = mock(HttpResponse.class);
        when(okBytesResponse.statusCode()).thenReturn(200);
        when(okBytesResponse.body()).thenReturn(new byte[]{1, 2, 3});
        when(mockClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofByteArray())))
                .thenReturn(okBytesResponse);

        MockMultipartFile file = new MockMultipartFile("file", "lazy.png", "image/png", new byte[]{1, 2, 3});
        String storedName = service.store(file);
        assertEquals(1, factoryInvocations.get(), "First storage operation should create HttpClient once");

        assertArrayEquals(new byte[]{1, 2, 3}, service.load(storedName).getInputStream().readAllBytes());
        service.delete(storedName);
        assertEquals(1, factoryInvocations.get(), "Subsequent operations should reuse the same HttpClient");
    }
}
