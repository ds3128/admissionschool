package org.darius.admission.entities;

import jakarta.persistence.*;
import lombok.*;
import org.darius.admission.common.enums.ConfirmationStatus;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "confirmation_requests")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ConfirmationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @ElementCollection
    @CollectionTable(name = "confirmation_accepted_choices",
            joinColumns = @JoinColumn(name = "confirmation_id"))
    @Column(name = "choice_id")
    @Builder.Default
    private List<Long> acceptedChoiceIds = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private Long confirmedChoiceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ConfirmationStatus status = ConfirmationStatus.PENDING;

    @Builder.Default
    private boolean autoConfirmed = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime confirmedAt;
}