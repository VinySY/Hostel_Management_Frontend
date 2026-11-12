package com.hostelsync.backend.config;

import com.hostelsync.backend.entity.Warden;
import com.hostelsync.backend.repository.DgmRepository;
import com.hostelsync.backend.repository.HostelDgmAssignmentRepository;
import com.hostelsync.backend.repository.HostelRepository;
import com.hostelsync.backend.repository.UserRepository;
import com.hostelsync.backend.repository.WardenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

/**
 * Verifies transaction rollback and enabled InitialBootstrapRunner startup
 * when a failure or uniqueness violation occurs mid-bootstrap.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:bootstrap_rollback_test;DB_CLOSE_DELAY=-1;MODE=MySQL;NON_KEYWORDS=YEAR",
        "app.seeder.enabled=false",
        "app.bootstrap.enabled=true",
        "app.bootstrap.hostel.name=Bootstrap Auto Hostel",
        "app.bootstrap.hostel.address=Campus Ave",
        "app.bootstrap.dgm.name=Bootstrap DGM",
        "app.bootstrap.dgm.email=bootstrap.dgm@university.edu",
        "app.bootstrap.dgm.password=BootstrapDgm!Pass#2026",
        "app.bootstrap.warden.name=Bootstrap Warden",
        "app.bootstrap.warden.email=bootstrap.warden@university.edu",
        "app.bootstrap.warden.password=BootstrapWdn!Pass#2026",
        "app.bootstrap.warden.employee-id=WDN-BOOT-1"
})
class InitialBootstrapRollbackTest {

    @Autowired
    private InitialBootstrapService initialBootstrapService;

    @Autowired(required = false)
    private InitialBootstrapRunner initialBootstrapRunner;

    @Autowired
    private HostelRepository hostelRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DgmRepository dgmRepository;

    @Autowired
    private HostelDgmAssignmentRepository hostelDgmAssignmentRepository;

    @MockitoSpyBean
    private WardenRepository wardenRepository;

    @BeforeEach
    void resetSpy() {
        reset(wardenRepository);
    }

    @Test
    void enabledRunner_bootstrapsOnceAndRollsBackAllRecordsOnSubsequentFailure() {
        // Runner was enabled and already executed once during context startup
        assertNotNull(initialBootstrapRunner);
        assertEquals(1, hostelRepository.count());
        assertEquals(2, userRepository.count());
        assertEquals(1, dgmRepository.count());
        assertEquals(1, hostelDgmAssignmentRepository.count());
        assertEquals(1, wardenRepository.count());

        // Repeated run skips cleanly
        assertFalse(initialBootstrapService.runBootstrap().executed());

        // Clear database and simulate mid-transaction uniqueness violation during Warden write
        hostelDgmAssignmentRepository.deleteAll();
        wardenRepository.deleteAll();
        dgmRepository.deleteAll();
        hostelRepository.deleteAll();
        userRepository.deleteAll();

        doThrow(new DataIntegrityViolationException("Unique constraint violation on wardens.employee_id"))
                .when(wardenRepository).saveAndFlush(any(Warden.class));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> initialBootstrapService.runBootstrap());
        assertTrue(ex.getMessage().contains("uniqueness or integrity violation"));

        // Verify ALL preceding writes (Hostel, DGM User, DGM profile, Assignment, Warden User) rolled back
        assertEquals(0, hostelRepository.count(), "Hostel creation must roll back");
        assertEquals(0, userRepository.count(), "User creation must roll back");
        assertEquals(0, dgmRepository.count(), "DGM creation must roll back");
        assertEquals(0, hostelDgmAssignmentRepository.count(), "HostelDgmAssignment must roll back");
        assertEquals(0, wardenRepository.count(), "Warden creation must roll back");
    }
}

