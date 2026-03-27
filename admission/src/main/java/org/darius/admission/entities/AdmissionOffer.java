package org.darius.admission.entities;

import jakarta.persistence.*;
import lombok.*;
import org.darius.admission.common.enums.OfferLevel;
import org.darius.admission.common.enums.OfferStatus;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "admission_offers")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AdmissionOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private AdmissionCampaign campaign;

    // Référence vers le User Service
    @Column(nullable = false)
    private Long filiereId;

    @Column(nullable = false, length = 150)
    private String filiereName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OfferLevel level;

    @Column(nullable = false)
    private LocalDate deadline;

    @Column(nullable = false)
    private int maxCapacity;

    @Column(nullable = false)
    @Builder.Default
    private int currentCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private int acceptedCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private int waitlistCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OfferStatus status = OfferStatus.OPEN;

    @OneToMany(mappedBy = "offer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RequiredDocument> requiredDocuments = new ArrayList<>();

    @OneToOne(mappedBy = "offer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ReviewCommission commission;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}