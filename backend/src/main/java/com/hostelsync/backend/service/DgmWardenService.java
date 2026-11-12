package com.hostelsync.backend.service;

import com.hostelsync.backend.dto.WardenCreateRequest;
import com.hostelsync.backend.dto.WardenFloorAssignRequest;
import com.hostelsync.backend.dto.WardenResponse;
import com.hostelsync.backend.dto.WardenUpdateRequest;
import com.hostelsync.backend.entity.*;
import com.hostelsync.backend.exception.BadRequestException;
import com.hostelsync.backend.exception.ResourceNotFoundException;
import com.hostelsync.backend.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DgmWardenService {

    private final WardenRepository wardenRepository;
    private final WardenFloorAssignmentRepository floorAssignmentRepository;
    private final HostelRepository hostelRepository;
    private final UserRepository userRepository;
    private final ComplaintRepository complaintRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public DgmWardenService(WardenRepository wardenRepository,
                           WardenFloorAssignmentRepository floorAssignmentRepository,
                           HostelRepository hostelRepository,
                           UserRepository userRepository,
                           ComplaintRepository complaintRepository,
                           PasswordEncoder passwordEncoder,
                           AuditService auditService) {
        this.wardenRepository = wardenRepository;
        this.floorAssignmentRepository = floorAssignmentRepository;
        this.hostelRepository = hostelRepository;
        this.userRepository = userRepository;
        this.complaintRepository = complaintRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<WardenResponse> getAllWardens(Long hostelId) {
        List<Warden> wardens = (hostelId != null)
                ? wardenRepository.findByHostelId(hostelId)
                : wardenRepository.findAll();

        return wardens.stream().map(this::toWardenResponse).toList();
    }

    @Transactional(readOnly = true)
    public WardenResponse getWardenById(Long id) {
        Warden warden = wardenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warden", id));
        return toWardenResponse(warden);
    }

    public WardenResponse createWarden(WardenCreateRequest request, String initiatedBy) {
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BadRequestException("Password is required");
        }
        if (request.getPassword().length() < 6 || request.getPassword().length() > 100) {
            throw new BadRequestException("Password must be between 6 and 100 characters");
        }

        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new IllegalArgumentException("User with email '" + request.getEmail() + "' already exists");
        }

        if (wardenRepository.findByEmployeeId(request.getEmployeeId().trim()).isPresent()) {
            throw new IllegalArgumentException("Warden with employee ID '" + request.getEmployeeId() + "' already exists");
        }

        Hostel hostel = hostelRepository.findById(request.getHostelId())
                .orElseThrow(() -> new ResourceNotFoundException("Hostel", request.getHostelId()));

        User user = new User(
                request.getName().trim(),
                request.getEmail().trim().toLowerCase(),
                passwordEncoder.encode(request.getPassword()),
                UserRole.WARDEN
        );
        user = userRepository.save(user);

        Warden warden = new Warden(user, request.getEmployeeId().trim(), hostel, request.getPhone());
        warden.setActive(true);
        warden = wardenRepository.save(warden);

        if (request.getAssignedFloors() != null && !request.getAssignedFloors().isEmpty()) {
            for (Integer floor : request.getAssignedFloors()) {
                WardenFloorAssignment fa = new WardenFloorAssignment(warden, hostel, floor);
                floorAssignmentRepository.save(fa);
            }
        }

        User actor = userRepository.findByEmail(initiatedBy).orElse(user);
        auditService.log("Warden", warden.getId(), "WARDEN_CREATED", actor, null, warden.getEmployeeId(), "Created warden account", null);

        return toWardenResponse(warden);
    }

    public WardenResponse updateWarden(Long id, WardenUpdateRequest request, String initiatedBy) {
        Warden warden = wardenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warden", id));

        User user = warden.getUser();
        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName().trim());
            userRepository.save(user);
        }

        if (request.getEmployeeId() != null && !request.getEmployeeId().isBlank()) {
            warden.setEmployeeId(request.getEmployeeId().trim());
        }

        if (request.getPhone() != null) {
            warden.setPhone(request.getPhone().trim());
        }

        if (request.getActive() != null) {
            warden.setActive(request.getActive());
        }

        if (request.getHostelId() != null) {
            Hostel hostel = hostelRepository.findById(request.getHostelId())
                    .orElseThrow(() -> new ResourceNotFoundException("Hostel", request.getHostelId()));
            warden.setHostel(hostel);
        }

        if (request.getAssignedFloors() != null) {
            floorAssignmentRepository.deleteByWardenId(warden.getId());
            if (warden.getHostel() != null) {
                for (Integer floor : request.getAssignedFloors()) {
                    WardenFloorAssignment fa = new WardenFloorAssignment(warden, warden.getHostel(), floor);
                    floorAssignmentRepository.save(fa);
                }
            }
        }

        warden = wardenRepository.save(warden);

        User actor = userRepository.findByEmail(initiatedBy).orElse(user);
        auditService.log("Warden", warden.getId(), "WARDEN_UPDATED", actor, null, warden.getEmployeeId(), "Updated warden details", null);

        return toWardenResponse(warden);
    }

    public WardenResponse assignFloors(Long id, WardenFloorAssignRequest request, String initiatedBy) {
        Warden warden = wardenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warden", id));

        Hostel hostel = hostelRepository.findById(request.getHostelId())
                .orElseThrow(() -> new ResourceNotFoundException("Hostel", request.getHostelId()));

        warden.setHostel(hostel);
        wardenRepository.save(warden);

        floorAssignmentRepository.deleteByWardenId(warden.getId());

        if (request.getFloors() != null) {
            for (Integer floor : request.getFloors()) {
                WardenFloorAssignment fa = new WardenFloorAssignment(warden, hostel, floor);
                floorAssignmentRepository.save(fa);
            }
        }

        User actor = userRepository.findByEmail(initiatedBy).orElse(warden.getUser());
        auditService.log("Warden", warden.getId(), "FLOORS_ASSIGNED", actor, null, request.getFloors().toString(), "Configured warden floor assignments", null);

        return toWardenResponse(warden);
    }

    public WardenResponse toggleWardenStatus(Long id, boolean active, String initiatedBy) {
        Warden warden = wardenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warden", id));

        warden.setActive(active);
        warden = wardenRepository.save(warden);

        User actor = userRepository.findByEmail(initiatedBy).orElse(warden.getUser());
        auditService.log("Warden", warden.getId(), active ? "WARDEN_ACTIVATED" : "WARDEN_DEACTIVATED", actor, null, null, "Warden status set to " + active, null);

        return toWardenResponse(warden);
    }

    public WardenResponse toWardenResponse(Warden w) {
        WardenResponse res = new WardenResponse();
        res.setId(w.getId());
        res.setUserId(w.getUser().getId());
        res.setName(w.getUser().getName());
        res.setEmail(w.getUser().getEmail());
        res.setEmployeeId(w.getEmployeeId());
        res.setPhone(w.getPhone());
        if (w.getHostel() != null) {
            res.setHostelId(w.getHostel().getId());
            res.setHostelName(w.getHostel().getName());
        }
        res.setActive(w.isActive());

        List<Integer> floors = floorAssignmentRepository.findByWardenId(w.getId())
                .stream()
                .map(WardenFloorAssignment::getFloor)
                .sorted()
                .toList();
        res.setAssignedFloors(floors);

        res.setActiveComplaintsCount(complaintRepository.countActiveComplaintsForWarden(w.getId()));
        res.setResolvedComplaintsCount(complaintRepository.countResolvedComplaintsForWarden(w.getId()));

        return res;
    }
}
