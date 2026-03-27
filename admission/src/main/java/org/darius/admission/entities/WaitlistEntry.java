package org.darius.admission.entities;

import jakarta.persistence.*;
import lombok.*;
import org.darius.admission.common.enums.WaitlistStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "waitlist_entries")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class WaitlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "choice_id", nullable = false)
    private ApplicationChoice choice;

    @Column(nullable = false)
    private Long offerId;

    @Column(nullable = false)
    private int rank;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private WaitlistStatus status = WaitlistStatus.WAITING;

    private LocalDateTime promotedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime notifiedAt;
}