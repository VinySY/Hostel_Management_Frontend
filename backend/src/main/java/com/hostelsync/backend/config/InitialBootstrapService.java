package com.hostelsync.backend.config;

import com.hostelsync.backend.entity.*;
import com.hostelsync.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Transactional, idempotent service for provisioning the initial production hostel,
 * primary DGM account, HostelDgmAssignment, and an optional initial Warden.
 */
@Service
public class InitialBootstrapService {

    public static final int MIN_PASSWORD_LENGTH = 12;
    public static final int MAX_PASSWORD_LENGTH = 100;
    private static final String BOOTSTRAP_ACTOR = "SYSTEM_BOOTSTRAP";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private static final Logger log = LoggerFactory.getLogger(InitialBootstrapService.class);

    private final InitialBootstrapProperties properties;
    private final HostelRepository hostelRepository;
    private final UserRepository userRepository;
    private final DgmRepository dgmRepository;
    private final WardenRepository wardenRepository;
    private final HostelDgmAssignmentRepository hostelDgmAssignmentRepository;
    private final PasswordEncoder passwordEncoder;

    public InitialBootstrapService(InitialBootstrapProperties properties,
                                   HostelRepository hostelRepository,
                                   UserRepository userRepository,
                                   DgmRepository dgmRepository,
                                   WardenRepository wardenRepository,
                                   HostelDgmAssignmentRepository hostelDgmAssignmentRepository,
                                   PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.hostelRepository = hostelRepository;
        this.userRepository = userRepository;
        this.dgmRepository = dgmRepository;
        this.wardenRepository = wardenRepository;
        this.hostelDgmAssignmentRepository = hostelDgmAssignmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Provisions initial records inside a single transaction when the database is empty.
     * Skips cleanly when already initialized, and fails clearly on partial state or invalid input.
     */
    @Transactional
    public BootstrapResult runBootstrap() {
        long userCount = userRepository.count();
        long dgmUserCount = userRepository.countByRole(UserRole.DGM);
        long wardenUserCount = userRepository.countByRole(UserRole.WARDEN);
        long dgmCount = dgmRepository.count();
        long hostelCount = hostelRepository.count();
        long wardenCount = wardenRepository.count();
        long assignmentCount = hostelDgmAssignmentRepository.count();

        boolean completelyEmpty = userCount == 0
                && dgmCount == 0
                && hostelCount == 0
                && wardenCount == 0
                && assignmentCount == 0;

        boolean cleanlyInitialized = hostelCount > 0
                && dgmCount > 0
                && dgmUserCount > 0
                && dgmUserCount == dgmCount
                && assignmentCount > 0
                && wardenUserCount == wardenCount;

        if (!completelyEmpty && cleanlyInitialized) {
            log.info("Initial bootstrap skipped: database is already initialized (hostels={}, dgms={}, assignments={}).",
                    hostelCount, dgmCount, assignmentCount);
            return BootstrapResult.skipped();
        }

        if (!completelyEmpty) {
            throw new IllegalStateException(String.format(
                    "Initial bootstrap aborted: database is in a partially initialized state "
                            + "(users=%d, dgmUsers=%d, dgms=%d, hostels=%d, wardens=%d, assignments=%d).",
                    userCount, dgmUserCount, dgmCount, hostelCount, wardenCount, assignmentCount));
        }

        ValidatedInput input = validateAndNormalize(properties);
        checkUniquenessBeforeWrite(input);

        try {
            Hostel hostel = new Hostel(input.hostelName(), input.hostelAddress());
            hostel.setActive(true);
            hostel = hostelRepository.saveAndFlush(hostel);

            User dgmUser = new User(
                    input.dgmName(),
                    input.dgmEmail(),
                    passwordEncoder.encode(input.dgmPassword()),
                    UserRole.DGM
            );
            dgmUser = userRepository.saveAndFlush(dgmUser);

            Dgm dgm = new Dgm();
            dgm.setUser(dgmUser);
            dgm.setEmployeeId(input.dgmEmployeeId());
            dgm.setDesignation(input.dgmDesignation());
            dgm.setOfficeLocation(input.dgmOfficeLocation());
            dgm.setContactExtension(input.dgmContactExtension());
            dgm = dgmRepository.saveAndFlush(dgm);

            HostelDgmAssignment assignment = new HostelDgmAssignment(hostel, dgm, BOOTSTRAP_ACTOR);
            assignment = hostelDgmAssignmentRepository.saveAndFlush(assignment);

            Long wardenId = null;
            if (input.includeWarden()) {
                User wardenUser = new User(
                        input.wardenName(),
                        input.wardenEmail(),
                        passwordEncoder.encode(input.wardenPassword()),
                        UserRole.WARDEN
                );
                wardenUser = userRepository.saveAndFlush(wardenUser);

                Warden warden = new Warden(wardenUser, input.wardenEmployeeId(), hostel, input.wardenPhone());
                warden.setActive(true);
                warden = wardenRepository.saveAndFlush(warden);
                wardenId = warden.getId();
            }

            log.info("Initial bootstrap completed: hostelId={}, dgmUserId={}, dgmId={}, assignmentId={}, wardenCreated={}.",
                    hostel.getId(), dgmUser.getId(), dgm.getId(), assignment.getId(), wardenId != null);

            return new BootstrapResult(true, hostel.getId(), dgmUser.getId(), dgm.getId(), assignment.getId(), wardenId);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException(
                    "Initial bootstrap failed due to a database uniqueness or integrity violation.", ex);
        }
    }

    private void checkUniquenessBeforeWrite(ValidatedInput input) {
        if (hostelRepository.existsByName(input.hostelName())) {
            throw new IllegalStateException("Initial bootstrap aborted: hostel name already exists.");
        }
        if (userRepository.existsByEmail(input.dgmEmail())) {
            throw new IllegalStateException("Initial bootstrap aborted: DGM email already exists.");
        }
        if (input.includeWarden()) {
            if (userRepository.existsByEmail(input.wardenEmail())) {
                throw new IllegalStateException("Initial bootstrap aborted: Warden email already exists.");
            }
            if (wardenRepository.findByEmployeeId(input.wardenEmployeeId()).isPresent()) {
                throw new IllegalStateException("Initial bootstrap aborted: Warden employee ID already exists.");
            }
        }
    }

    private ValidatedInput validateAndNormalize(InitialBootstrapProperties props) {
        if (props == null) {
            throw new IllegalStateException("Initial bootstrap configuration is missing.");
        }

        InitialBootstrapProperties.HostelProps hostel = props.getHostel();
        String hostelName = requireText(hostel != null ? hostel.getName() : null,
                "BOOTSTRAP_HOSTEL_NAME", 150);
        String hostelAddress = optionalText(hostel != null ? hostel.getAddress() : null,
                "BOOTSTRAP_HOSTEL_ADDRESS", 300);

        InitialBootstrapProperties.DgmProps dgm = props.getDgm();
        String dgmName = requireText(dgm != null ? dgm.getName() : null,
                "BOOTSTRAP_DGM_NAME", 120);
        String dgmEmail = requireEmail(dgm != null ? dgm.getEmail() : null,
                "BOOTSTRAP_DGM_EMAIL");
        String dgmPassword = requirePassword(dgm != null ? dgm.getPassword() : null,
                "BOOTSTRAP_DGM_PASSWORD");
        String dgmEmployeeId = optionalText(dgm != null ? dgm.getEmployeeId() : null,
                "BOOTSTRAP_DGM_EMPLOYEE_ID", 50);
        String dgmDesignation = optionalText(dgm != null ? dgm.getDesignation() : null,
                "BOOTSTRAP_DGM_DESIGNATION", 150);
        String dgmOfficeLocation = optionalText(dgm != null ? dgm.getOfficeLocation() : null,
                "BOOTSTRAP_DGM_OFFICE_LOCATION", 150);
        String dgmContactExtension = optionalText(dgm != null ? dgm.getContactExtension() : null,
                "BOOTSTRAP_DGM_CONTACT_EXTENSION", 20);

        InitialBootstrapProperties.WardenProps warden = props.getWarden();
        boolean anyWardenInput = warden != null && (hasText(warden.getName())
                || hasText(warden.getEmail())
                || hasText(warden.getPassword())
                || hasText(warden.getEmployeeId())
                || hasText(warden.getPhone()));

        String wardenName = null;
        String wardenEmail = null;
        String wardenPassword = null;
        String wardenEmployeeId = null;
        String wardenPhone = null;

        if (anyWardenInput) {
            wardenName = requireText(warden.getName(), "BOOTSTRAP_WARDEN_NAME", 120);
            wardenEmail = requireEmail(warden.getEmail(), "BOOTSTRAP_WARDEN_EMAIL");
            wardenPassword = requirePassword(warden.getPassword(), "BOOTSTRAP_WARDEN_PASSWORD");
            wardenEmployeeId = requireText(warden.getEmployeeId(), "BOOTSTRAP_WARDEN_EMPLOYEE_ID", 50);
            wardenPhone = optionalText(warden.getPhone(), "BOOTSTRAP_WARDEN_PHONE", 30);

            if (wardenEmail.equals(dgmEmail)) {
                throw new IllegalStateException(
                        "BOOTSTRAP_WARDEN_EMAIL must be distinct from BOOTSTRAP_DGM_EMAIL.");
            }
        }

        return new ValidatedInput(
                hostelName,
                hostelAddress,
                dgmName,
                dgmEmail,
                dgmPassword,
                dgmEmployeeId,
                dgmDesignation,
                dgmOfficeLocation,
                dgmContactExtension,
                anyWardenInput,
                wardenName,
                wardenEmail,
                wardenPassword,
                wardenEmployeeId,
                wardenPhone
        );
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String requireText(String value, String varName, int maxLength) {
        if (!hasText(value)) {
            throw new IllegalStateException(varName + " is required for initial bootstrap.");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalStateException(varName + " must not exceed " + maxLength + " characters.");
        }
        return trimmed;
    }

    private static String optionalText(String value, String varName, int maxLength) {
        if (!hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalStateException(varName + " must not exceed " + maxLength + " characters.");
        }
        return trimmed;
    }

    private static String requireEmail(String value, String varName) {
        String trimmed = requireText(value, varName, 200).toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalStateException(varName + " must be a valid email address.");
        }
        return trimmed;
    }

    private static String requirePassword(String value, String varName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(varName + " is required for initial bootstrap.");
        }
        if (value.length() < MIN_PASSWORD_LENGTH || value.length() > MAX_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    varName + " must be between " + MIN_PASSWORD_LENGTH + " and " + MAX_PASSWORD_LENGTH + " characters.");
        }
        return value;
    }

    private record ValidatedInput(
            String hostelName,
            String hostelAddress,
            String dgmName,
            String dgmEmail,
            String dgmPassword,
            String dgmEmployeeId,
            String dgmDesignation,
            String dgmOfficeLocation,
            String dgmContactExtension,
            boolean includeWarden,
            String wardenName,
            String wardenEmail,
            String wardenPassword,
            String wardenEmployeeId,
            String wardenPhone
    ) {}

    public record BootstrapResult(
            boolean executed,
            Long hostelId,
            Long dgmUserId,
            Long dgmId,
            Long assignmentId,
            Long wardenId
    ) {
        public static BootstrapResult skipped() {
            return new BootstrapResult(false, null, null, null, null, null);
        }
    }
}

