package org.darius.userservice.services.impl;

import lombok.RequiredArgsConstructor;
import org.darius.userservice.common.dtos.responses.PageResponse;
import org.darius.userservice.common.dtos.responses.SearchResultResponse;
import org.darius.userservice.entities.UserProfile;
import org.darius.userservice.exceptions.InvalidOperationException;
import org.darius.userservice.repositories.StudentRepository;
import org.darius.userservice.repositories.TeacherRepository;
import org.darius.userservice.repositories.StaffRepository;
import org.darius.userservice.repositories.UserProfileRepository;
import org.darius.userservice.services.UserSearchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserSearchServiceImpl implements UserSearchService {

    private final UserProfileRepository profileRepository;
    private final StudentRepository     studentRepository;
    private final TeacherRepository     teacherRepository;
    private final StaffRepository       staffRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SearchResultResponse> search(String query, int page, int size) {
        if (query == null || query.trim().length() < 2) {
            throw new InvalidOperationException(
                    "La recherche nécessite au minimum 2 caractères"
            );
        }

        String trimmed = query.trim();
        List<SearchResultResponse> results = new ArrayList<>();

        // 1. Recherche dans les profils (prénom, nom, email)
        List<UserProfile> profiles = profileRepository.searchByQuery(trimmed);

        for (UserProfile profile : profiles) {
            // Recherche étudiant
            studentRepository.findByProfileId(profile.getId()).ifPresent(student ->
                    results.add(SearchResultResponse.builder()
                            .id(student.getId())
                            .profileId(profile.getId())
                            .firstName(profile.getFirstName())
                            .lastName(profile.getLastName())
                            .avatarUrl(profile.getAvatarUrl())
                            .identifier(student.getStudentNumber())
                            .userType("STUDENT")
                            .contextInfo(student.getFiliere() != null
                                    ? student.getFiliere().getName() : "")
                            .build()
                    )
            );

            // Recherche enseignant
            teacherRepository.findByProfileId(profile.getId()).ifPresent(teacher ->
                    results.add(SearchResultResponse.builder()
                            .id(teacher.getId())
                            .profileId(profile.getId())
                            .firstName(profile.getFirstName())
                            .lastName(profile.getLastName())
                            .avatarUrl(profile.getAvatarUrl())
                            .identifier(teacher.getEmployeeNumber())
                            .userType("TEACHER")
                            .contextInfo(teacher.getDepartment() != null
                                    ? teacher.getDepartment().getName() : "")
                            .build()
                    )
            );

            // Recherche personnel
            staffRepository.findByProfileId(profile.getId()).ifPresent(staff ->
                    results.add(SearchResultResponse.builder()
                            .id(staff.getId())
                            .profileId(profile.getId())
                            .firstName(profile.getFirstName())
                            .lastName(profile.getLastName())
                            .avatarUrl(profile.getAvatarUrl())
                            .identifier(staff.getStaffNumber())
                            .userType("STAFF")
                            .contextInfo(staff.getDepartment() != null
                                    ? staff.getDepartment().getName() : "")
                            .build()
                    )
            );
        }

        // 2. Recherche supplémentaire par numéro matricule
        studentRepository.searchByStudentNumber(trimmed).forEach(student -> {
            boolean alreadyAdded = results.stream()
                    .anyMatch(r -> r.getId().equals(student.getId()));
            if (!alreadyAdded) {
                profileRepository.findById(student.getProfileId()).ifPresent(profile ->
                        results.add(SearchResultResponse.builder()
                                .id(student.getId())
                                .profileId(profile.getId())
                                .firstName(profile.getFirstName())
                                .lastName(profile.getLastName())
                                .avatarUrl(profile.getAvatarUrl())
                                .identifier(student.getStudentNumber())
                                .userType("STUDENT")
                                .contextInfo(student.getFiliere() != null
                                        ? student.getFiliere().getName() : "")
                                .build()
                        )
                );
            }
        });

        // 3. Pagination manuelle sur les résultats
        int total = results.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex   = Math.min(fromIndex + size, total);
        List<SearchResultResponse> pageContent = results.subList(fromIndex, toIndex);

        return PageResponse.<SearchResultResponse>builder()
                .content(pageContent)
                .page(page)
                .size(size)
                .totalElements(total)
                .totalPages((int) Math.ceil((double) total / size))
                .last(toIndex >= total)
                .build();
    }
}
