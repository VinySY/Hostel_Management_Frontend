package com.hostelsync.backend.config;

import com.hostelsync.backend.entity.UserRole;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Authoritative server-side registry of dedicated Demo Mode identities.
 */
@Component
public class DemoAccountRegistry {

    public static final String DEMO_EMAIL_DOMAIN = "@demo.hostelsync.local";

    private final DemoProperties demoProperties;

    public DemoAccountRegistry(DemoProperties demoProperties) {
        this.demoProperties = demoProperties;
    }

    public boolean isPublicDemoEnabled() {
        return demoProperties.isEnabled();
    }

    public Optional<String> getDemoEmailForRole(UserRole role) {
        if (role == null) {
            return Optional.empty();
        }
        String raw = switch (role) {
            case STUDENT -> demoProperties.getStudentEmail();
            case WARDEN -> demoProperties.getWardenEmail();
            case DGM -> demoProperties.getDgmEmail();
        };
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(raw.trim().toLowerCase(Locale.ROOT));
    }

    public Set<String> getDemoEmails() {
        return Set.of(
                normalize(demoProperties.getStudentEmail()),
                normalize(demoProperties.getWardenEmail()),
                normalize(demoProperties.getDgmEmail())
        );
    }

    public boolean isDemoEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        String normalized = normalize(email);
        return getDemoEmails().contains(normalized) || normalized.endsWith(DEMO_EMAIL_DOMAIN);
    }

    public boolean isDemoHostelName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        return normalize(name).equals(normalize(getDemoHostelName()));
    }

    public String getDemoHostelName() {
        return demoProperties.getHostelName() != null
                ? demoProperties.getHostelName().trim()
                : "HostelSync Showcase Hostel (Demo)";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}

