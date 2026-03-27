package org.darius.payment.entities;

import jakarta.persistence.*;
import lombok.*;
import org.darius.payment.common.enums.InstallmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "installments")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Installment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private PaymentSchedule schedule;

    @Column(name = "installment_number", nullable = false)
    private int installmentNumber;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InstallmentStatus status = InstallmentStatus.PENDING;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // Référence vers le Payment qui a réglé cette échéance
    @Column(name = "payment_id")
    private String paymentId;
}