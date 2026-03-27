package org.darius.payment.entities;

import jakarta.persistence.*;
import lombok.*;
import org.darius.payment.common.enums.InvoiceStatus;
import org.darius.payment.common.enums.InvoiceType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"student_id", "academic_year", "semester", "type"}
        ),
        indexes = {
                @Index(name = "idx_invoice_student", columnList = "student_id"),
                @Index(name = "idx_invoice_status",  columnList = "status")
        })
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "student_id", nullable = false)
    private String studentId;

    @Column(name = "academic_year", nullable = false, length = 20)
    private String academicYear;

    @Column(nullable = false, length = 10)
    private String semester;  // S1, S2, ANNUEL

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceType type;

    // Montant brut standard (avant déduction bourse)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    // Déduction bourse (0 si aucune bourse active)
    @Column(name = "scholarship_deduction", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal scholarshipDeduction = BigDecimal.ZERO;

    // Montant net à payer = amount - scholarshipDeduction
    @Column(name = "net_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal netAmount;

    // Montant déjà réglé
    @Column(name = "paid_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    // Solde restant = netAmount - paidAmount
    @Column(name = "remaining_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal remainingAmount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.PENDING;

    @Column(name = "has_schedule", nullable = false)
    @Builder.Default
    private boolean hasSchedule = false;

    @Column(name = "currency", length = 10)
    @Builder.Default
    private String currency = "EUR";

    @OneToOne(mappedBy = "invoice", cascade = CascadeType.ALL)
    private PaymentSchedule schedule;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Méthodes de calcul ────────────────────────────────────────────────────

    public void recalculate() {
        this.netAmount = this.amount.subtract(this.scholarshipDeduction);
        this.remainingAmount = this.netAmount.subtract(this.paidAmount);
    }

    public void applyPayment(BigDecimal payedAmount) {
        this.paidAmount = this.paidAmount.add(payedAmount);
        this.remainingAmount = this.netAmount.subtract(this.paidAmount);
        if (this.remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            this.status = InvoiceStatus.PAID;
        } else if (this.paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.status = InvoiceStatus.PARTIAL;
        }
    }
}