package org.darius.course.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "attendances",
        uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "student_id"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    // Référence vers Student dans le User Service
    @Column(name = "student_id", nullable = false)
    private String studentId;

    @Column(nullable = false)
    @Builder.Default
    private boolean present = false;

    @Column(columnDefinition = "TEXT")
    private String justification;

    @Column(name = "justified_at")
    private LocalDateTime justifiedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}