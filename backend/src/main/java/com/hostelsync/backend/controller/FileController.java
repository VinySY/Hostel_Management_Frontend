package com.hostelsync.backend.controller;

import com.hostelsync.backend.entity.User;
import com.hostelsync.backend.entity.UserRole;
import com.hostelsync.backend.exception.ForbiddenException;
import com.hostelsync.backend.exception.ResourceNotFoundException;
import com.hostelsync.backend.repository.ComplaintAttachmentRepository;
import com.hostelsync.backend.repository.UserRepository;
import com.hostelsync.backend.service.DemoScopeService;
import com.hostelsync.backend.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * GET /api/files/{storedFileName}  — authenticated users only.
 *
 * Students may only download files attached to their own complaints or their own profile avatar.
 * Wardens and DGMs may download complaint attachments and student photos.
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService fileStorageService;
    private final ComplaintAttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private DemoScopeService demoScopeService;

    public FileController(FileStorageService fileStorageService,
                          ComplaintAttachmentRepository attachmentRepository,
                          UserRepository userRepository) {
        this.fileStorageService = fileStorageService;
        this.attachmentRepository = attachmentRepository;
        this.userRepository = userRepository;
    }

    @Autowired(required = false)
    public void setDemoScopeService(DemoScopeService demoScopeService) {
        this.demoScopeService = demoScopeService;
    }

    @GetMapping("/{storedFileName:.+}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String storedFileName,
            @AuthenticationPrincipal UserDetails principal) {

        User caller = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        // Find if it's an attachment record
        var attachmentOpt = attachmentRepository.findByStoredFileName(storedFileName);

        String contentType = "application/octet-stream";
        String originalFileName = storedFileName;

        if (attachmentOpt.isPresent()) {
            var attachment = attachmentOpt.get();
            // Students may only access files from their own complaints
            if (caller.getRole() == UserRole.STUDENT) {
                Long complaintOwnerId = attachment.getComplaint().getStudent().getUser().getId();
                if (!caller.getId().equals(complaintOwnerId)) {
                    throw new ForbiddenException("You are not authorised to access this file.");
                }
            }
            if (demoScopeService != null) {
                demoScopeService.assertComplaintAccessible(attachment.getComplaint());
            }
            if (attachment.getContentType() != null) {
                contentType = attachment.getContentType();
            }
            if (attachment.getOriginalFileName() != null) {
                originalFileName = attachment.getOriginalFileName();
            }
        } else {
            // Avatar or non-attachment file: students may only access their own avatar
            if (caller.getRole() == UserRole.STUDENT) {
                String expectedAvatarPath = "/api/files/" + storedFileName;
                String callerAvatar = caller.getAvatarUrl();
                if (callerAvatar == null || (!expectedAvatarPath.equals(callerAvatar) && !storedFileName.equals(callerAvatar))) {
                    throw new ForbiddenException("You are not authorised to access this file.");
                }
            }
            if (demoScopeService != null) {
                demoScopeService.assertAvatarAccessible(storedFileName);
            }

            String lower = storedFileName.toLowerCase();
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) contentType = "image/jpeg";
            else if (lower.endsWith(".png")) contentType = "image/png";
            else if (lower.endsWith(".webp")) contentType = "image/webp";
            else if (lower.endsWith(".mp4")) contentType = "video/mp4";
            else if (lower.endsWith(".webm")) contentType = "video/webm";
            else if (lower.endsWith(".mov")) contentType = "video/quicktime";
        }

        Resource resource = fileStorageService.load(storedFileName);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + originalFileName.replace("\"", "") + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }
}
