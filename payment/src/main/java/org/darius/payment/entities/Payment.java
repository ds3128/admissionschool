package org.darius.payment.entities;

import jakarta.persistence.*;
import lombok.*;
import org.darius.payment.common.enums.PaymentMethod;
import org.darius.payment.common.enums.PaymentStatus;
import org.darius.payment.common.enums.PaymentType;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments",
        indexes = {
                @Index(name = "idx_payment_user",        columnList = "user_id"),
                @Index(name = "idx_payment_application",  columnList = "application_id"),
                @Index(name = "idx_payment_reference",    columnList = "reference", unique = true)
        })
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    // Référence vers Admission Service — nullable (uniquement FRAIS_DOSSIER)
    @Column(name = "application_id")
    private String applicationId;

    // Référence vers Invoice — nullable (uniquement FRAIS_SCOLARITE)
    @Column(name = "invoice_id")
    private String invoiceId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "XAF";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    // Référence interne : PAY-{year}-{seq}
    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String reference;

    // Référence retournée par la passerelle externe
    @Column(name = "external_reference", length = 200, nullable = true)
    private String externalReference;

    @Column(length = 300)
    private String description;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    private LocalDateTime paidAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}