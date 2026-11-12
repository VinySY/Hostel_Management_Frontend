package com.hostelsync.backend.config;

import com.hostelsync.backend.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security configuration:
 * - Stateless JWT authentication
 * - Role-based endpoint protection
 * - Configurable CORS (never wildcard with credentials)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Value("${app.cors.allowed-origins:http://localhost:*,http://127.0.0.1:*,http://10.*:*,http://192.168.*:*,http://172.*:*,http://localhost:3000,http://localhost:3001,http://127.0.0.1:3000,http://127.0.0.1:3001,http://10.200.50.197:3000,http://10.200.50.197:3001,http://10.63.156.120:3000,http://10.63.156.120:3001}")
    private String allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Allow preflight OPTIONS requests explicitly
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Public endpoints
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/auth/ping").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/health").permitAll()

                // File download — authenticated users only (further auth in controller)
                .requestMatchers(HttpMethod.GET, "/api/files/**").authenticated()

                // Student-only endpoints
                .requestMatchers(HttpMethod.GET,  "/api/complaints/my").hasRole("STUDENT")
                .requestMatchers(HttpMethod.POST, "/api/complaints").hasRole("STUDENT")
                .requestMatchers(HttpMethod.POST, "/api/complaints/*/escalate").hasRole("STUDENT")
                .requestMatchers(HttpMethod.POST, "/api/complaints/*/reopen").hasRole("STUDENT")

                // Warden-only endpoints
                .requestMatchers(HttpMethod.GET,    "/api/students").hasRole("WARDEN")
                .requestMatchers(HttpMethod.POST,   "/api/students").hasRole("WARDEN")
                .requestMatchers(HttpMethod.DELETE, "/api/students/**").hasRole("WARDEN")
                .requestMatchers(HttpMethod.PATCH,  "/api/complaints/*/status").hasRole("WARDEN")
                .requestMatchers(HttpMethod.GET,    "/api/dashboard/stats").hasRole("WARDEN")
                .requestMatchers(HttpMethod.POST,   "/api/complaints/*/assign").hasRole("WARDEN")
                .requestMatchers(HttpMethod.GET,    "/api/complaints/*/history").hasAnyRole("WARDEN", "DGM")

                // DGM-only endpoints
                .requestMatchers("/api/dgm/**").hasRole("DGM")
                .requestMatchers("/api/escalations/**").hasRole("DGM")

                // SLA configurations - reads for authenticated users, modifications for administrative roles
                .requestMatchers(HttpMethod.GET,  "/api/sla-configurations/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/sla-configurations/**").hasAnyRole("WARDEN", "DGM")
                .requestMatchers(HttpMethod.PUT,  "/api/sla-configurations/**").hasAnyRole("WARDEN", "DGM")

                // Notifications - authenticated users only
                .requestMatchers("/api/notifications/**").authenticated()

                // Audit logs - authenticated users only
                .requestMatchers("/api/audit-logs/**").authenticated()

                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Parse comma-separated origins/patterns from config (supports patterns with credentials)
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        config.setAllowedOriginPatterns(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));
        config.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
