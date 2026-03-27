package org.darius.course.entities;

import jakarta.persistence.*;
import lombok.*;
import org.darius.course.enums.GroupType;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "student_groups",
        indexes = {
                @Index(name = "idx_group_semester", columnList = "semester_id"),
                @Index(name = "idx_group_filiere",  columnList = "filiere_id")
        })
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class StudentGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private GroupType type;

    // Référence vers StudyLevel dans le User Service
    @Column(name = "level_id")
    private Long levelId;

    // Référence vers Filiere dans le User Service
    @Column(name = "filiere_id")
    private Long filiereId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @Column(name = "max_size", nullable = false)
    @Builder.Default
    private int maxSize = 30;

    // IDs des étudiants membres — références cross-service vers User Service
    @ElementCollection
    @CollectionTable(
            name = "student_group_members",
            joinColumns = @JoinColumn(name = "group_id")
    )
    @Column(name = "student_id")
    @Builder.Default
    private List<String> studentIds = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}