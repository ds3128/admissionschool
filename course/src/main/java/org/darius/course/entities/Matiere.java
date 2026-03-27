package org.darius.course.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "matieres",
        uniqueConstraints = @UniqueConstraint(columnNames = "code"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Matiere {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teaching_unit_id", nullable = false)
    private TeachingUnit teachingUnit;

    // Référence vers Department dans le User Service
    @Column(name = "department_id")
    private Long departmentId;

    @Column(nullable = false)
    private double coefficient;

    @Column(name = "total_hours", nullable = false)
    private int totalHours;

    @Column(name = "hours_cm", nullable = false)
    @Builder.Default
    private int hoursCM = 0;

    @Column(name = "hours_td", nullable = false)
    @Builder.Default
    private int hoursTD = 0;

    @Column(name = "hours_tp", nullable = false)
    @Builder.Default
    private int hoursTP = 0;

    // Seuil de présence minimum en pourcentage (défaut 80%)
    @Column(name = "attendance_threshold", nullable = false)
    @Builder.Default
    private double attendanceThreshold = 80.0;

    @OneToMany(mappedBy = "matiere", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CourseResource> resources = new ArrayList<>();

    @OneToMany(mappedBy = "matiere", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Evaluation> evaluations = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}