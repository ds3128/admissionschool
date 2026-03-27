package org.darius.admission.entities;

import jakarta.persistence.*;
import lombok.*;
import org.darius.admission.common.enums.ApprovalStatus;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "thesis_director_approvals")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ThesisDirectorApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "choice_id", nullable = false)
    private ApplicationChoice choice;

    @Column(nullable = false)
    private String directorId;

    @Column(columnDefinition = "TEXT")
    private String researchProject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @CreationTimestamp
    @Column(name = "requested_at", updatable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime respondedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;
}