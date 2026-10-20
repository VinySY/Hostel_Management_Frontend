package com.hostelsync.backend.controller;

import com.hostelsync.backend.entity.User;
import com.hostelsync.backend.entity.UserRole;
import com.hostelsync.backend.exception.ForbiddenException;
import com.hostelsync.backend.exception.ResourceNotFoundException;
import com.hostelsync.backend.repository.ComplaintAttachmentRepository;
import com.hostelsync.backend.repository.UserRepository;
import com.hostelsync.backend.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * GET /api/files/{storedFileName}  — authenticated users only.
 *
 * Students may only download files attached to their own complaints.
 * Wardens may download any file.
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService fileStorageService;
    private final ComplaintAttachmentRepository attachmentRepository;
    private final UserRepository userRepository;

    public FileController(FileStorageService fileStorageService,
                          ComplaintAttachmentRepository attachmentRepository,
                          UserRepository userRepository) {
        this.fileStorageService = fileStorageService;
        this.attachmentRepository = attachmentRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/{storedFileName:.+}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String storedFileName,
            @AuthenticationPrincipal UserDetails principal) {

        User caller = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        // Find the attachment record
        var attachment = attachmentRepository.findByStoredFileName(storedFileName)
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + storedFileName));

        // Students may only access files from their own complaints
        if (caller.getRole() == UserRole.STUDENT) {
            Long complaintOwnerId = attachment.getComplaint().getStudent().getUser().getId();
            if (!caller.getId().equals(complaintOwnerId)) {
                throw new ForbiddenException("You are not authorised to access this file.");
            }
        }

        Resource resource = fileStorageService.load(storedFileName);

        String contentType = attachment.getContentType() != null
                ? attachment.getContentType()
                : "application/octet-stream";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + attachment.getOriginalFileName() + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }
}
