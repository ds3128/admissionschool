package org.darius.course.entities;

import jakarta.persistence.*;
import lombok.*;
import org.darius.course.enums.TeachingRole;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "teacher_assignments",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"teacher_id", "matiere_id", "role", "semester_id"}
        ))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TeacherAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Référence vers Teacher dans le User Service
    @Column(name = "teacher_id", nullable = false)
    private String teacherId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matiere_id", nullable = false)
    private Matiere matiere;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TeachingRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @Column(name = "assigned_hours", nullable = false)
    private int assignedHours;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}