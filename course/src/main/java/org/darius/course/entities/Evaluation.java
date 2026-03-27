package org.darius.course.entities;

import jakarta.persistence.*;
import lombok.*;
import org.darius.course.enums.EvalType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "evaluations",
        indexes = {
                @Index(name = "idx_eval_matiere",   columnList = "matiere_id"),
                @Index(name = "idx_eval_semester",  columnList = "semester_id")
        })
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EvalType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matiere_id", nullable = false)
    private Matiere matiere;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @Column
    private LocalDate date;

    // Coefficient dans la note finale de la matière — somme des coefficients = 1.0
    @Column(nullable = false)
    private double coefficient;

    // Note maximale (défaut 20.0)
    @Column(name = "max_score", nullable = false)
    @Builder.Default
    private double maxScore = 20.0;

    // Notes visibles par les étudiants
    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private boolean isPublished = false;

    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<EvaluationAttachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Grade> grades = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}