package com.hostelsync.backend.service;

import com.hostelsync.backend.dto.*;
import com.hostelsync.backend.entity.Dgm;
import com.hostelsync.backend.entity.Hostel;
import com.hostelsync.backend.entity.HostelDgmAssignment;
import com.hostelsync.backend.entity.StudentStatus;
import com.hostelsync.backend.entity.User;
import com.hostelsync.backend.entity.UserRole;
import com.hostelsync.backend.exception.BadRequestException;
import com.hostelsync.backend.exception.ResourceNotFoundException;
import com.hostelsync.backend.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class HostelService {

    private final HostelRepository hostelRepository;
    private final RoomRepository roomRepository;
    private final StudentRepository studentRepository;
    private final WardenRepository wardenRepository;
    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final DgmRepository dgmRepository;
    private final HostelDgmAssignmentRepository hostelDgmAssignmentRepository;
    private final MappingService mappingService;

    public HostelService(HostelRepository hostelRepository,
                         RoomRepository roomRepository,
                         StudentRepository studentRepository,
                         WardenRepository wardenRepository,
                         ComplaintRepository complaintRepository,
                         UserRepository userRepository,
                         DgmRepository dgmRepository,
                         HostelDgmAssignmentRepository hostelDgmAssignmentRepository,
                         MappingService mappingService) {
        this.hostelRepository = hostelRepository;
        this.roomRepository = roomRepository;
        this.studentRepository = studentRepository;
        this.wardenRepository = wardenRepository;
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.dgmRepository = dgmRepository;
        this.hostelDgmAssignmentRepository = hostelDgmAssignmentRepository;
        this.mappingService = mappingService;
    }

    @Transactional(readOnly = true)
    public List<HostelResponse> getAllHostels(boolean includeInactive) {
        List<Hostel> list = includeInactive ? hostelRepository.findAll() : hostelRepository.findByActiveTrue();
        return list.stream().map(this::toHostelResponse).toList();
    }

    @Transactional(readOnly = true)
    public HostelResponse getHostelById(Long id) {
        Hostel hostel = hostelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hostel", id));
        return toHostelResponse(hostel);
    }

    @Transactional(readOnly = true)
    public HostelDetailResponse getHostelDetails(Long id) {
        Hostel hostel = hostelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hostel", id));

        HostelResponse summary = toHostelResponse(hostel);

        List<RoomResponse> rooms = roomRepository.findByHostelId(id)
                .stream()
                .map(mappingService::toRoomResponse)
                .toList();

        List<WardenResponse> wardens = wardenRepository.findByHostelId(id)
                .stream()
                .map(w -> {
                    WardenResponse wr = new WardenResponse();
                    wr.setId(w.getId());
                    wr.setUserId(w.getUser().getId());
                    wr.setName(w.getUser().getName());
                    wr.setEmail(w.getUser().getEmail());
                    wr.setEmployeeId(w.getEmployeeId());
                    wr.setPhone(w.getPhone());
                    wr.setHostelId(hostel.getId());
                    wr.setHostelName(hostel.getName());
                    wr.setActive(w.isActive());
                    wr.setAssignedFloors(w.getFloorAssignments().stream().map(com.hostelsync.backend.entity.WardenFloorAssignment::getFloor).toList());
                    wr.setActiveComplaintsCount(complaintRepository.countActiveComplaintsForWarden(w.getId()));
                    wr.setResolvedComplaintsCount(complaintRepository.countResolvedComplaintsForWarden(w.getId()));
                    return wr;
                })
                .toList();

        List<ComplaintResponse> complaints = complaintRepository.findByHostelId(id)
                .stream()
                .limit(20)
                .map(mappingService::toComplaintResponse)
                .toList();

        return new HostelDetailResponse(summary, rooms, wardens, complaints);
    }

    public HostelResponse createHostel(HostelCreateRequest request) {
        return createHostel(request, resolveAuthenticatedEmail());
    }

    public HostelResponse createHostel(HostelCreateRequest request, String creatorEmail) {
        if (hostelRepository.existsByName(request.getName().trim())) {
            throw new IllegalArgumentException("Hostel with name '" + request.getName() + "' already exists");
        }

        Hostel hostel = new Hostel(request.getName().trim(), request.getAddress() != null ? request.getAddress().trim() : null);
        hostel.setActive(true);
        Hostel saved = hostelRepository.save(hostel);

        String effectiveEmail = (creatorEmail != null && !creatorEmail.isBlank())
                ? creatorEmail.trim()
                : resolveAuthenticatedEmail();

        if (effectiveEmail != null) {
            userRepository.findByEmailIgnoreCase(effectiveEmail).ifPresent(creator -> {
                if (creator.getRole() == UserRole.DGM) {
                    Dgm dgm = dgmRepository.findByUserId(creator.getId())
                            .orElseThrow(() -> new BadRequestException("Authenticated DGM user does not have a DGM profile."));
                    if (hostelDgmAssignmentRepository.findByHostelIdAndDgmId(saved.getId(), dgm.getId()).isEmpty()) {
                        HostelDgmAssignment assignment = new HostelDgmAssignment(saved, dgm, creator.getEmail());
                        hostelDgmAssignmentRepository.save(assignment);
                    }
                }
            });
        }

        return toHostelResponse(saved);
    }

    private String resolveAuthenticatedEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        String name = auth.getName();
        if (name == null || name.isBlank() || "anonymousUser".equals(name)) {
            return null;
        }
        return name;
    }

    public HostelResponse updateHostel(Long id, HostelUpdateRequest request) {
        Hostel hostel = hostelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hostel", id));

        if (request.getName() != null && !request.getName().isBlank()) {
            hostel.setName(request.getName().trim());
        }
        if (request.getAddress() != null) {
            hostel.setAddress(request.getAddress().trim());
        }
        if (request.getActive() != null) {
            hostel.setActive(request.getActive());
        }

        Hostel saved = hostelRepository.save(hostel);
        return toHostelResponse(saved);
    }

    public HostelResponse toggleHostelActive(Long id, boolean active) {
        Hostel hostel = hostelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hostel", id));
        hostel.setActive(active);
        return toHostelResponse(hostelRepository.save(hostel));
    }

    public HostelResponse toHostelResponse(Hostel h) {
        HostelResponse res = new HostelResponse();
        res.setId(h.getId());
        res.setName(h.getName());
        res.setAddress(h.getAddress());
        res.setActive(h.isActive());

        long roomCount = roomRepository.countByHostelId(h.getId());
        long cap = roomRepository.sumCapacityByHostelId(h.getId());
        long occ = studentRepository.countByRoomHostelIdAndStatus(h.getId(), StudentStatus.Active);
        long wardens = wardenRepository.findByHostelId(h.getId()).size();
        long activeComplaints = complaintRepository.countActiveComplaintsForHostel(h.getId());

        res.setTotalRooms(roomCount);
        res.setTotalCapacity(cap);
        res.setTotalOccupancy(occ);
        res.setTotalStudents(occ);
        res.setWardensCount(wardens);
        res.setActiveComplaintsCount(activeComplaints);

        return res;
    }
}
