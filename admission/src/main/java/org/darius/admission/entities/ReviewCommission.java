package org.darius.admission.entities;

import jakarta.persistence.*;
import lombok.*;
import org.darius.admission.common.enums.CommissionType;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "review_commissions")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ReviewCommission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offer_id", nullable = false)
    private AdmissionOffer offer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommissionType type;

    private String presidentId;

    @Column(nullable = false)
    @Builder.Default
    private int quorum = 3;

    @OneToMany(mappedBy = "commission", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CommissionMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "commission", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CommissionVote> votes = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}