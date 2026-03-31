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

    @Column(nullable = false, unique = true)
    private String profileId;

    @Column(nullable = false, unique = true, length = 20)
    private String studentNumber;

    @Column(nullable = false)
    private int enrollmentYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filiere_id", nullable = false)
    private Filiere filiere;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_level_id", nullable = false)
    private StudyLevel currentLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudentStatus status = StudentStatus.ACTIVE;

    @Column(length = 100)
    private String admissionApplicationId;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<StudentAcademicHistory> academicHistory = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}