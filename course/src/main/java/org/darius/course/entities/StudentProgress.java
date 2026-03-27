package org.darius.course.entities;

import jakarta.persistence.*;
import lombok.*;
import org.darius.course.enums.Mention;
import org.darius.course.enums.ProgressStatus;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_progress",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"student_id", "semester_id"}
        ),
        indexes = {
                @Index(name = "idx_progress_student",  columnList = "student_id"),
                @Index(name = "idx_progress_semester", columnList = "semester_id"),
                @Index(name = "idx_progress_status",   columnList = "status")
        })
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class StudentProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Référence vers Student dans le User Service
    @Column(name = "student_id", nullable = false)
    private String studentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @Column(name = "semester_average", nullable = false)
    private double semesterAverage;

    @Column(name = "credits_obtained", nullable = false)
    @Builder.Default
    private int creditsObtained = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ProgressStatus status = ProgressStatus.AJOURNE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Mention mention = Mention.INSUFFISANT;

    // Rang dans le groupe promotion
    @Column(nullable = false)
    @Builder.Default
    private int rank = 0;

    @Column(name = "is_admis", nullable = false)
    @Builder.Default
    private boolean isAdmis = false;

    @Column(name = "computed_at")
    private LocalDateTime computedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
