package org.darius.userservice.entities;

import jakarta.persistence.*;
import lombok.*;
import org.darius.userservice.common.enums.FiliereStatus;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "filieres", indexes = {
        @Index(name = "idx_filiere_code", columnList = "code", unique = true),
        @Index(name = "idx_filiere_department", columnList = "department_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Filiere {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(nullable = false)
    private int durationYears;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FiliereStatus status = FiliereStatus.ACTIVE;

    @OneToMany(mappedBy = "filiere", cascade = {CascadeType.ALL, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @Builder.Default
    private List<StudyLevel> studyLevels = new ArrayList<>();

    @OneToMany(mappedBy = "filiere", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Student> students = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}