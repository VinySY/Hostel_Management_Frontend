package com.hostelsync.backend.controller;

import com.hostelsync.backend.dto.NoticeCreateRequest;
import com.hostelsync.backend.dto.NoticeResponse;
import com.hostelsync.backend.service.NoticeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping
    public ResponseEntity<List<NoticeResponse>> getNotices(Authentication authentication,
                                                           @RequestParam(value = "all", defaultValue = "false") boolean all) {
        String email = authentication != null ? authentication.getName() : "";
        boolean isStaff = authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_WARDEN") || a.equals("ROLE_DGM"));

        if (all && isStaff) {
            return ResponseEntity.ok(noticeService.getAllNotices());
        }
        return ResponseEntity.ok(noticeService.getActiveNotices(email));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('WARDEN', 'DGM')")
    public ResponseEntity<NoticeResponse> createNotice(@Valid @RequestBody NoticeCreateRequest request,
                                                       Authentication authentication) {
        String email = authentication != null ? authentication.getName() : "";
        String role = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DGM")) ? "DGM" : "WARDEN";

        return ResponseEntity.status(HttpStatus.CREATED).body(noticeService.createNotice(request, email, role));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('WARDEN', 'DGM')")
    public ResponseEntity<NoticeResponse> toggleNoticeStatus(@PathVariable Long id,
                                                            @RequestBody Map<String, Boolean> body) {
        boolean active = body.getOrDefault("active", true);
        return ResponseEntity.ok(noticeService.toggleNoticeActive(id, active));
    }
}
