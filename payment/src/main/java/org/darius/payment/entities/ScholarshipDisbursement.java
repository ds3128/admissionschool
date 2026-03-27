package org.darius.payment.entities;

import jakarta.persistence.*;
import lombok.*;
import org.darius.payment.common.enums.DisbursementStatus;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "scholarship_disbursements",
        indexes = {
                @Index(name = "idx_disbursement_scholarship", columnList = "scholarship_id"),
                @Index(name = "idx_disbursement_status",      columnList = "status")
        })
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ScholarshipDisbursement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scholarship_id", nullable = false)
    private Scholarship scholarship;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    // Format : yyyy-MM (ex : 2026-03)
    @Column(nullable = false, length = 10)
    private String period;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DisbursementStatus status = DisbursementStatus.SCHEDULED;

    // Référence vers le Payment créé au moment du versement
    @Column(name = "payment_id")
    private String paymentId;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}