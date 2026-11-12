package com.hostelsync.backend.security;

import com.hostelsync.backend.dto.ErrorResponse;
import com.hostelsync.backend.service.DemoScopeService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Set;

/**
 * Enforces strict server-side read-only mode for Demo Mode users.
 * Blocks all POST, PUT, PATCH, and DELETE API requests with HTTP 403 Forbidden.
 */
@Component
public class DemoReadOnlyFilter extends OncePerRequestFilter {

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final DemoScopeService demoScopeService;
    private final ObjectMapper objectMapper;

    public DemoReadOnlyFilter(DemoScopeService demoScopeService, ObjectMapper objectMapper) {
        this.demoScopeService = demoScopeService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod();
        String path = request.getRequestURI();

        // Allow re-authentication / demo role switching even if a demo Bearer header is attached
        if ("/api/auth/login".equals(path) || "/api/auth/demo-login".equals(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (WRITE_METHODS.contains(method) && demoScopeService.isCurrentCallerDemo()) {
            ErrorResponse payload = new ErrorResponse(
                    HttpStatus.FORBIDDEN.value(),
                    "Forbidden",
                    "Demo accounts are read-only. Modifying, creating, or deleting records is disabled in Demo Mode.",
                    path
            );
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setCharacterEncoding("UTF-8");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(payload));
            return;
        }

        filterChain.doFilter(request, response);
    }
}

