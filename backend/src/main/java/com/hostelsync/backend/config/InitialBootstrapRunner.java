package com.hostelsync.backend.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Runs one-time initial bootstrap at application startup only when
 * app.bootstrap.enabled=true (ENABLE_INITIAL_BOOTSTRAP=true).
 */
@Component
@ConditionalOnProperty(name = "app.bootstrap.enabled", havingValue = "true")
public class InitialBootstrapRunner implements ApplicationRunner {

    private final InitialBootstrapService initialBootstrapService;

    public InitialBootstrapRunner(InitialBootstrapService initialBootstrapService) {
        this.initialBootstrapService = initialBootstrapService;
    }

    @Override
    public void run(ApplicationArguments args) {
        initialBootstrapService.runBootstrap();
    }
}

