package org.darius.course.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "teaching_units",
        uniqueConstraints = @UniqueConstraint(columnNames = "code"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TeachingUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int credits;

    // Référence vers StudyLevel dans le User Service
    @Column(name = "study_level_id", nullable = false)
    private Long studyLevelId;

    // 1 ou 2
    @Column(name = "semester_number", nullable = false)
    private int semesterNumber;

    @Column(nullable = false)
    private double coefficient;

    @OneToMany(mappedBy = "teachingUnit", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Matiere> matieres = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}