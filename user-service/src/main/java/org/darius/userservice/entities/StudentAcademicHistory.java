package org.darius.userservice.entities;

import jakarta.persistence.*;
import lombok.*;
import org.darius.userservice.common.enums.HistoryChangeReason;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

//@Entity
//@Table(name = "student_academic_history")
//@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
//public class StudentAcademicHistory {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    // Relation JPA — Student est dans la même base de données
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "student_id", nullable = false)
//    private Student student;
//
//    // On garde filiereId et levelId comme Long (pas d'objet JPA)
//    // car l'historique doit rester consultable même si la Filiere
//    // ou le StudyLevel sont archivés ou modifiés
//    @Column(nullable = false)
//    private Long filiereId;
//
//    @Column(nullable = false)
//    private Long levelId;
//
//    // Noms dénormalisés pour consultation sans jointure
//    @Column(length = 150)
//    private String filiereName;
//
//    @Column(length = 50)
//    private String levelLabel;
//
//    @Column(nullable = false, length = 20)
//    private String academicYear;
//
//    @Column(nullable = false)
//    private LocalDate startDate;
//
//    // null si enregistrement courant
//    private LocalDate endDate;
//
//    @Enumerated(EnumType.STRING)
//    private HistoryChangeReason reason;
//
//    @CreationTimestamp
//    @Column(updatable = false)
//    private LocalDateTime createdAt;
//}

@Entity
@Table(name = "student_academic_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StudentAcademicHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relation JPA — Student est dans la même base de données
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    // On garde filiereId et levelId comme Long (pas d'objet JPA)
    // car l'historique doit rester consultable même si la Filiere
    // ou le StudyLevel sont archivés ou modifiés
    @Column(nullable = false)
    private Long filiereId;

    @Column(nullable = false)
    private Long levelId;

    // Noms dénormalisés pour consultation sans jointure
    @Column(length = 150)
    private String filiereName;

    @Column(length = 50)
    private String levelLabel;

    @Column(nullable = false, length = 20)
    private String academicYear;

    @Column(nullable = false)
    private LocalDate startDate;

    // null si enregistrement courant
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private HistoryChangeReason reason;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}