package com.hostelsync.backend.config;

import com.hostelsync.backend.entity.Dgm;
import com.hostelsync.backend.entity.Hostel;
import com.hostelsync.backend.entity.HostelDgmAssignment;
import com.hostelsync.backend.entity.User;
import com.hostelsync.backend.entity.UserRole;
import com.hostelsync.backend.entity.Warden;
import com.hostelsync.backend.repository.DgmRepository;
import com.hostelsync.backend.repository.HostelDgmAssignmentRepository;
import com.hostelsync.backend.repository.HostelRepository;
import com.hostelsync.backend.repository.UserRepository;
import com.hostelsync.backend.repository.WardenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies one-time InitialBootstrapService & InitialBootstrapRunner behavior:
 * defaults, validation, BCrypt encoding, optional Warden, idempotency,
 * partially initialized DB detection, and bean activation flags.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:bootstrap_service_test;DB_CLOSE_DELAY=-1;MODE=MySQL;NON_KEYWORDS=YEAR",
        "app.seeder.enabled=false",
        "app.bootstrap.enabled=false"
})
class InitialBootstrapServiceTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private HostelRepository hostelRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DgmRepository dgmRepository;

    @Autowired
    private WardenRepository wardenRepository;

    @Autowired
    private HostelDgmAssignmentRepository hostelDgmAssignmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        hostelDgmAssignmentRepository.deleteAll();
        wardenRepository.deleteAll();
        dgmRepository.deleteAll();
        hostelRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void defaultConfiguration_disablesBootstrapRunnerAndDataSeeder() {
        assertFalse(applicationContext.containsBean("initialBootstrapRunner"),
                "InitialBootstrapRunner must not be registered when ENABLE_INITIAL_BOOTSTRAP defaults to false");
        assertFalse(applicationContext.containsBean("dataSeeder"),
                "DataSeeder must remain disabled by default");
    }

    @Test
    void successfulBootstrap_withoutOptionalWarden_createsHostelDgmAndAssignment() {
        String rawDgmPassword = "DgmProd!StrongPassword#2026";
        InitialBootstrapProperties props = validHostelAndDgmProps(rawDgmPassword);
        InitialBootstrapService service = newService(props);

        InitialBootstrapService.BootstrapResult result = service.runBootstrap();

        assertTrue(result.executed());
        assertNotNull(result.hostelId());
        assertNotNull(result.dgmUserId());
        assertNotNull(result.dgmId());
        assertNotNull(result.assignmentId());
        assertNull(result.wardenId());

        assertEquals(1, hostelRepository.count());
        assertEquals(1, userRepository.count());
        assertEquals(1, dgmRepository.count());
        assertEquals(1, hostelDgmAssignmentRepository.count());
        assertEquals(0, wardenRepository.count());

        Hostel hostel = hostelRepository.findById(result.hostelId()).orElseThrow();
        assertEquals("North Campus Hostel", hostel.getName());
        assertEquals("1 University Way", hostel.getAddress());
        assertTrue(hostel.isActive());

        User dgmUser = userRepository.findByEmail("director.prod@university.edu").orElseThrow();
        assertEquals(UserRole.DGM, dgmUser.getRole());
        assertNotEquals(rawDgmPassword, dgmUser.getPasswordHash());
        assertTrue(dgmUser.getPasswordHash().startsWith("$2"));
        assertTrue(passwordEncoder.matches(rawDgmPassword, dgmUser.getPasswordHash()));

        Dgm dgm = dgmRepository.findByUserId(dgmUser.getId()).orElseThrow();
        assertEquals("DGM-001", dgm.getEmployeeId());
        assertEquals("Director of Hostels", dgm.getDesignation());

        List<HostelDgmAssignment> assignments = hostelDgmAssignmentRepository.findByDgmId(dgm.getId());
        assertEquals(1, assignments.size());
        assertEquals(hostel.getId(), assignments.get(0).getHostel().getId());
        assertEquals("SYSTEM_BOOTSTRAP", assignments.get(0).getAssignedBy());
    }

    @Test
    void successfulBootstrap_withOptionalWarden_createsWardenLinkedToHostel() {
        String rawDgmPassword = "DgmProd!StrongPassword#2026";
        String rawWardenPassword = "WardenProd!StrongPassword#2026";
        InitialBootstrapProperties props = validHostelAndDgmProps(rawDgmPassword);
        props.getWarden().setName("Chief Warden");
        props.getWarden().setEmail("WARDEN.PROD@University.edu");
        props.getWarden().setPassword(rawWardenPassword);
        props.getWarden().setEmployeeId("WDN-001");
        props.getWarden().setPhone("+91 98765 00001");

        InitialBootstrapService.BootstrapResult result = newService(props).runBootstrap();

        assertTrue(result.executed());
        assertNotNull(result.wardenId());
        assertEquals(2, userRepository.count());
        assertEquals(1, wardenRepository.count());

        User wardenUser = userRepository.findByEmail("warden.prod@university.edu").orElseThrow();
        assertEquals(UserRole.WARDEN, wardenUser.getRole());
        assertTrue(passwordEncoder.matches(rawWardenPassword, wardenUser.getPasswordHash()));

        Warden warden = wardenRepository.findById(result.wardenId()).orElseThrow();
        assertEquals("WDN-001", warden.getEmployeeId());
        assertEquals(result.hostelId(), warden.getHostel().getId());
        assertTrue(warden.isActive());
    }

    @Test
    void missingRequiredEnvironmentValues_failsClearlyWithNoWrites() {
        InitialBootstrapProperties missingHostel = validHostelAndDgmProps("DgmProd!StrongPassword#2026");
        missingHostel.getHostel().setName("   ");
        IllegalStateException hostelEx = assertThrows(IllegalStateException.class,
                () -> newService(missingHostel).runBootstrap());
        assertTrue(hostelEx.getMessage().contains("BOOTSTRAP_HOSTEL_NAME"));

        InitialBootstrapProperties missingDgmEmail = validHostelAndDgmProps("DgmProd!StrongPassword#2026");
        missingDgmEmail.getDgm().setEmail(null);
        IllegalStateException emailEx = assertThrows(IllegalStateException.class,
                () -> newService(missingDgmEmail).runBootstrap());
        assertTrue(emailEx.getMessage().contains("BOOTSTRAP_DGM_EMAIL"));

        InitialBootstrapProperties missingDgmPassword = validHostelAndDgmProps("   ");
        IllegalStateException pwEx = assertThrows(IllegalStateException.class,
                () -> newService(missingDgmPassword).runBootstrap());
        assertTrue(pwEx.getMessage().contains("BOOTSTRAP_DGM_PASSWORD"));

        assertEquals(0, hostelRepository.count());
        assertEquals(0, userRepository.count());
        assertEquals(0, dgmRepository.count());
    }

    @Test
    void invalidOrShortPasswords_andPartialWardenConfig_failWithoutLeakingSecrets() {
        String shortSecret = "TooShort11";
        InitialBootstrapProperties shortPw = validHostelAndDgmProps(shortSecret);
        IllegalStateException shortEx = assertThrows(IllegalStateException.class,
                () -> newService(shortPw).runBootstrap());
        assertTrue(shortEx.getMessage().contains("BOOTSTRAP_DGM_PASSWORD"));
        assertFalse(shortEx.getMessage().contains(shortSecret));

        InitialBootstrapProperties badEmail = validHostelAndDgmProps("DgmProd!StrongPassword#2026");
        badEmail.getDgm().setEmail("not-an-email");
        assertThrows(IllegalStateException.class, () -> newService(badEmail).runBootstrap());

        InitialBootstrapProperties partialWarden = validHostelAndDgmProps("DgmProd!StrongPassword#2026");
        partialWarden.getWarden().setName("Partial Warden");
        partialWarden.getWarden().setEmail("partial.warden@university.edu");
        // missing warden password and employeeId
        IllegalStateException partialEx = assertThrows(IllegalStateException.class,
                () -> newService(partialWarden).runBootstrap());
        assertTrue(partialEx.getMessage().contains("BOOTSTRAP_WARDEN_PASSWORD"));

        InitialBootstrapProperties sameEmail = validHostelAndDgmProps("DgmProd!StrongPassword#2026");
        sameEmail.getWarden().setName("Dup Email Warden");
        sameEmail.getWarden().setEmail("director.prod@university.edu");
        sameEmail.getWarden().setPassword("WardenProd!StrongPassword#2026");
        sameEmail.getWarden().setEmployeeId("WDN-999");
        assertThrows(IllegalStateException.class, () -> newService(sameEmail).runBootstrap());

        assertEquals(0, hostelRepository.count());
        assertEquals(0, userRepository.count());
    }

    @Test
    void repeatedExecutionAndInitializedDatabase_skipSafelyWithoutDuplicates() {
        InitialBootstrapProperties props = validHostelAndDgmProps("DgmProd!StrongPassword#2026");
        InitialBootstrapService service = newService(props);

        InitialBootstrapService.BootstrapResult first = service.runBootstrap();
        assertTrue(first.executed());

        // Repeated execution via runner even if credentials were later unset from env
        InitialBootstrapRunner runner = new InitialBootstrapRunner(newService(new InitialBootstrapProperties()));
        assertDoesNotThrow(() -> runner.run(new DefaultApplicationArguments()));

        InitialBootstrapService.BootstrapResult second = service.runBootstrap();
        assertFalse(second.executed());

        assertEquals(1, hostelRepository.count());
        assertEquals(1, userRepository.count());
        assertEquals(1, dgmRepository.count());
        assertEquals(1, hostelDgmAssignmentRepository.count());
    }

    @Test
    void partiallyInitializedDatabase_failsClearlyAndCreatesNoRecords() {
        // Case 1: Hostel exists without any DGM or assignment
        hostelRepository.save(new Hostel("Orphan Hostel", "Somewhere"));

        InitialBootstrapProperties props = validHostelAndDgmProps("DgmProd!StrongPassword#2026");
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> newService(props).runBootstrap());
        assertTrue(ex.getMessage().contains("partially initialized"));

        assertEquals(1, hostelRepository.count());
        assertEquals(0, dgmRepository.count());
        assertEquals(0, hostelDgmAssignmentRepository.count());

        // Case 2: Hostel + DGM exist, but HostelDgmAssignment missing
        User dgmUser = userRepository.save(new User(
                "Orphan DGM",
                "orphan.dgm@university.edu",
                passwordEncoder.encode("AnotherStrongPass#2026"),
                UserRole.DGM));
        dgmRepository.save(new Dgm(dgmUser, "DGM-ORPHAN", "Director"));

        IllegalStateException ex2 = assertThrows(IllegalStateException.class,
                () -> newService(props).runBootstrap());
        assertTrue(ex2.getMessage().contains("partially initialized"));
        assertEquals(0, hostelDgmAssignmentRepository.count());
    }

    private InitialBootstrapService newService(InitialBootstrapProperties props) {
        return new InitialBootstrapService(
                props,
                hostelRepository,
                userRepository,
                dgmRepository,
                wardenRepository,
                hostelDgmAssignmentRepository,
                passwordEncoder
        );
    }

    private InitialBootstrapProperties validHostelAndDgmProps(String dgmPassword) {
        InitialBootstrapProperties props = new InitialBootstrapProperties();
        props.setEnabled(true);
        props.getHostel().setName("North Campus Hostel");
        props.getHostel().setAddress("1 University Way");
        props.getDgm().setName("Dr. Production DGM");
        props.getDgm().setEmail("Director.Prod@University.edu");
        props.getDgm().setPassword(dgmPassword);
        props.getDgm().setEmployeeId("DGM-001");
        props.getDgm().setDesignation("Director of Hostels");
        return props;
    }
}

