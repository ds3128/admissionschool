package org.darius.admission.entities;

import jakarta.persistence.*;
import lombok.*;
import org.darius.admission.common.enums.ApplicationStatus;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "applications",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "campaign_id"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private AdmissionCampaign campaign;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.DRAFT;

    @Column(nullable = false, length = 20)
    private String academicYear;

    private LocalDateTime submittedAt;
    private LocalDateTime paidAt;
    private LocalDateTime lastStatusChange;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ApplicationChoice> choices = new ArrayList<>();

    @OneToOne(mappedBy = "application", cascade = CascadeType.ALL)
    private Dossier dossier;

    @OneToOne(mappedBy = "application", cascade = CascadeType.ALL)
    private CandidateProfile candidateProfile;

    @OneToOne(mappedBy = "application", cascade = CascadeType.ALL)
    private AdmissionPayment payment;

    @OneToOne(mappedBy = "application", cascade = CascadeType.ALL)
    private ConfirmationRequest confirmationRequest;

    @Column(updatable = true)
    private LocalDateTime confirmationDeadline;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ApplicationStatusHistory> statusHistory = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}