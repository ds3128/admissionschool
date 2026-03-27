package org.darius.payment.entities;

import jakarta.persistence.*;
import lombok.*;
import org.darius.payment.common.enums.DisbursementFrequency;
import org.darius.payment.common.enums.ScholarshipSource;
import org.darius.payment.common.enums.ScholarshipStatus;
import org.darius.payment.common.enums.ScholarshipType;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "scholarships",
        indexes = {
                @Index(name = "idx_scholarship_student", columnList = "student_id"),
                @Index(name = "idx_scholarship_status",  columnList = "status")
        })
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Scholarship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private String studentId;

    @Column(name = "academic_year", nullable = false, length = 20)
    private String academicYear;

    // Montant annuel total
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    // Montant par versement (calculé selon fréquence)
    @Column(name = "disbursement_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal disbursementAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "disbursement_frequency", nullable = false, length = 20)
    private DisbursementFrequency disbursementFrequency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScholarshipType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ScholarshipStatus status = ScholarshipStatus.PENDING;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(columnDefinition = "TEXT")
    private String conditions;

    // Moyenne minimale requise — uniquement pour MERITE
    @Column(name = "minimum_grade", precision = 4, scale = 2)
    private BigDecimal minimumGrade;

    @Column(name = "suspension_reason", columnDefinition = "TEXT")
    private String suspensionReason;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ScholarshipSource source = ScholarshipSource.INSTITUTIONNELLE;

    @OneToMany(mappedBy = "scholarship", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ScholarshipDisbursement> disbursements = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}