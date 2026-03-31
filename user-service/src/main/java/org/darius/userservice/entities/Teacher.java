package org.darius.userservice.entities;

import jakarta.persistence.*;
import lombok.*;
import org.darius.userservice.common.enums.AcademicGrade;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "teachers", indexes = {
        @Index(name = "idx_teacher_employee_number", columnList = "employeeNumber", unique = true),
        @Index(name = "idx_teacher_profile",         columnList = "profileId",      unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Cross-service : référence vers UserProfile.id
    @Column(nullable = false, unique = true)
    private String profileId;

    // Cross-service : référence vers Users.id dans l'Auth Service
    @Column(nullable = false, unique = true)
    private String userId;

    @Column(nullable = false, unique = true, length = 20)
    private String employeeNumber;

    @Column(length = 150)
    private String speciality;

    @Enumerated(EnumType.STRING)
    private AcademicGrade grade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(length = 150)
    private String diploma;

    @Column(nullable = false)
    private int maxHoursPerWeek;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private String createdBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}