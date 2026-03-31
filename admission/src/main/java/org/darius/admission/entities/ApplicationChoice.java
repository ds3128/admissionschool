package org.darius.admission.entities;

import jakarta.persistence.*;
import lombok.*;
import org.darius.admission.common.enums.ChoiceStatus;
import org.darius.admission.common.enums.OfferLevel;

import java.time.LocalDateTime;

@Entity
@Table(name = "application_choices",
        uniqueConstraints = @UniqueConstraint(columnNames = {"application_id", "offer_id"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ApplicationChoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offer_id", nullable = false)
    private AdmissionOffer offer;

    // Dénormalisé pour l'historique
    @Column(nullable = false)
    private Long filiereId;

    @Column(nullable = false, length = 150)
    private String filiereName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OfferLevel level;

    @Column(nullable = false)
    private int choiceOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private ChoiceStatus status = ChoiceStatus.PENDING_ADMIN;

    private LocalDateTime decidedAt;
    private String decidedBy;

    @Column(columnDefinition = "TEXT")
    private String decisionReason;

    @OneToOne(mappedBy = "choice", cascade = CascadeType.ALL)
    private WaitlistEntry waitlistEntry;

    @OneToOne(mappedBy = "choice", cascade = CascadeType.ALL)
    private Interview interview;

    @OneToOne(mappedBy = "choice", cascade = CascadeType.ALL)
    private ThesisDirectorApproval thesisDirectorApproval;
}