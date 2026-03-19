package org.darius.userservice.entities;

import jakarta.persistence.*;
import lombok.*;
import org.darius.userservice.common.enums.StudentStatus;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "students", indexes = {
        @Index(name = "idx_student_number",  columnList = "studentNumber", unique = true),
        @Index(name = "idx_student_profile", columnList = "profileId",     unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Cross-service : référence vers UserProfile.id (même service — relation JPA possible)
    // On garde String car UUID et OneToOne sur profileId
    @Column(nullable = false, unique = true)
    private String profileId;

    @Column(nullable = false, unique = true, length = 20)
    private String studentNumber;

    @Column(nullable = false)
    private int enrollmentYear;

    // Relation JPA — Filiere est dans la même base de données
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filiere_id", nullable = false)
    private Filiere filiere;

    // Relation JPA — StudyLevel est dans la même base de données
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_level_id", nullable = false)
    private StudyLevel currentLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudentStatus status = StudentStatus.ACTIVE;

    // Cross-service : référence vers Application.id dans l'Admission Service
    @Column(length = 100)
    private String admissionApplicationId;

    // Relation JPA — historique académique dans la même base
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<StudentAcademicHistory> academicHistory = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}