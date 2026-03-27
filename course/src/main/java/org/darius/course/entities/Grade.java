package org.darius.course.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "grades",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "evaluation_id"}),
        indexes = {
                @Index(name = "idx_grade_student",    columnList = "student_id"),
                @Index(name = "idx_grade_eval",       columnList = "evaluation_id"),
                @Index(name = "idx_grade_semester",   columnList = "semester_id")
        })
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Référence vers Student dans le User Service
    @Column(name = "student_id", nullable = false)
    private String studentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private Evaluation evaluation;

    // Dénormalisé pour faciliter les requêtes
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matiere_id", nullable = false)
    private Matiere matiere;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @Column(nullable = false)
    private double score;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "graded_by", nullable = false)
    private String gradedBy;

    @CreationTimestamp
    @Column(name = "graded_at", updatable = false)
    private LocalDateTime gradedAt;

    // Helper : score ramené sur 20
    public double getScoreOn20() {
        return (score / evaluation.getMaxScore()) * 20.0;
    }
}