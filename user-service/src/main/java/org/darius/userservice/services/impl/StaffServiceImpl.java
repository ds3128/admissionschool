package org.darius.userservice.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.darius.userservice.common.dtos.requests.CreateStaffRequest;
import org.darius.userservice.common.dtos.responses.PageResponse;
import org.darius.userservice.common.dtos.responses.StaffResponse;
import org.darius.userservice.entities.Department;
import org.darius.userservice.entities.Staff;
import org.darius.userservice.entities.UserProfile;
import org.darius.userservice.events.produces.StaffCreationRequestedEvent;
import org.darius.userservice.events.produces.StaffProfileCreatedEvent;
import org.darius.userservice.exceptions.DuplicateResourceException;
import org.darius.userservice.exceptions.InvalidOperationException;
import org.darius.userservice.exceptions.UserNotFoundException;
import org.darius.userservice.kafka.UserEventProducer;
import org.darius.userservice.mappers.UserMapper;
import org.darius.userservice.repositories.DepartmentRepository;
import org.darius.userservice.repositories.StaffRepository;
import org.darius.userservice.repositories.UserProfileRepository;
import org.darius.userservice.services.StaffService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class StaffServiceImpl implements StaffService {

    private final StaffRepository       staffRepository;
    private final UserProfileRepository profileRepository;
    private final DepartmentRepository  departmentRepository;
    private final UserEventProducer eventProducer;
    private final UserMapper userMapper;

    public StaffServiceImpl(StaffRepository staffRepository, UserProfileRepository profileRepository, DepartmentRepository departmentRepository, UserEventProducer eventProducer, UserMapper userMapper) {
        this.staffRepository = staffRepository;
        this.profileRepository = profileRepository;
        this.departmentRepository = departmentRepository;
        this.eventProducer = eventProducer;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public StaffResponse createStaff(CreateStaffRequest request, String createdByUserId) {
        log.info("Création du profil personnel : email={}", request.getPersonalEmail());

        if (profileRepository.existsByPersonalEmail(request.getPersonalEmail())) {
            throw new DuplicateResourceException(
                    "L'email '" + request.getPersonalEmail() + "' est déjà utilisé"
            );
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new UserNotFoundException(
                        "Département introuvable : id=" + request.getDepartmentId()
                ));

        String staffNumber = generateStaffNumber();
        String requestId   = UUID.randomUUID().toString();

        // Demande de création de compte vers Auth Service
        eventProducer.publishStaffCreationRequested(
                StaffCreationRequestedEvent.builder()
                        .requestId(requestId)
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .personalEmail(request.getPersonalEmail())
                        .role("ADMIN_SCHOLAR")
                        .build()
        );

        UserProfile profile = UserProfile.builder()
                .userId(requestId)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .personalEmail(request.getPersonalEmail())
                .build();

        profile = profileRepository.save(profile);

        Staff staff = Staff.builder()
                .profileId(profile.getId())
                .userId(requestId)
                .staffNumber(staffNumber)
                .position(request.getPosition())
                .department(department)
                .active(true)
                .createdBy(createdByUserId)
                .build();

        staff = staffRepository.save(staff);

        eventProducer.publishStaffProfileCreated(StaffProfileCreatedEvent.builder()
                .staffId(staff.getId())
                .userId(staff.getUserId())
                .departmentId(department.getId())
                .staffNumber(staffNumber)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .personalEmail(request.getPersonalEmail())
                .build()
        );

        log.info("Profil personnel créé : id={}, staffNumber={}", staff.getId(), staffNumber);
        return userMapper.toStaffResponse(staff, profile);
    }

    @Override
    @Transactional(readOnly = true)
    public StaffResponse getStaffById(String staffId) {
        Staff staff = findStaffOrThrow(staffId);
        UserProfile profile = findProfileOrThrow(staff.getProfileId());
        return userMapper.toStaffResponse(staff, profile);
    }

    @Override
    @Transactional(readOnly = true)
    public StaffResponse getMyStaffProfile(String userEmail) {
        UserProfile profile = profileRepository.findByPersonalEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException(
                        "Profil introuvable pour email=" + userEmail
                ));
        Staff staff = staffRepository.findByProfileId(profile.getId())
                .orElseThrow(() -> new UserNotFoundException(
                        "Dossier personnel introuvable pour profileId=" + profile.getId()
                ));
        return userMapper.toStaffResponse(staff, profile);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StaffResponse> getStaffMembers(
            Long departmentId, Boolean isActive, int page, int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Staff> staffPage = staffRepository.findWithFilters(departmentId, isActive, pageable);

        List<StaffResponse> content = staffPage.getContent().stream()
                .map(s -> {
                    UserProfile p = profileRepository.findById(s.getProfileId()).orElse(null);
                    return userMapper.toStaffResponse(s, p);
                })
                .toList();

        return PageResponse.<StaffResponse>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(staffPage.getTotalElements())
                .totalPages(staffPage.getTotalPages())
                .last(staffPage.isLast())
                .build();
    }

    @Override
    @Transactional
    public StaffResponse deactivateStaff(String staffId) {
        Staff staff = findStaffOrThrow(staffId);

        if (!staff.isActive()) {
            throw new InvalidOperationException("Le membre du personnel est déjà inactif");
        }

        staff.setActive(false);
        staff = staffRepository.save(staff);

        log.info("Personnel désactivé : staffId={}", staffId);
        UserProfile profile = findProfileOrThrow(staff.getProfileId());
        return userMapper.toStaffResponse(staff, profile);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Staff findStaffOrThrow(String staffId) {
        return staffRepository.findById(staffId)
                .orElseThrow(() -> new UserNotFoundException(
                        "Personnel introuvable : id=" + staffId
                ));
    }

    private UserProfile findProfileOrThrow(String profileId) {
        return profileRepository.findById(profileId)
                .orElseThrow(() -> new UserNotFoundException(
                        "Profil introuvable : id=" + profileId
                ));
    }

    private String generateStaffNumber() {
        int year = Year.now().getValue();
        long count = staffRepository.count() + 1;
        return String.format("STF-%d-%05d", year, count);
    }
}