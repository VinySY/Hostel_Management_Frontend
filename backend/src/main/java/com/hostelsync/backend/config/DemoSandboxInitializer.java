package com.hostelsync.backend.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Seeds the isolated Demo Mode showcase hostel and accounts only when
 * ENABLE_DEMO_SANDBOX_SEED=true. Never touches real institutional data.
 */
@Component
@Order(2)
@ConditionalOnProperty(name = "app.demo.seed-enabled", havingValue = "true")
public class DemoSandboxInitializer implements ApplicationRunner {

    private final DemoSandboxService demoSandboxService;

    public DemoSandboxInitializer(DemoSandboxService demoSandboxService) {
        this.demoSandboxService = demoSandboxService;
    }

    @Override
    public void run(ApplicationArguments args) {
        demoSandboxService.ensureDemoSandboxInitialized();
    }
}

