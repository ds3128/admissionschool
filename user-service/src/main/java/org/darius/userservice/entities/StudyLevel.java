package org.darius.userservice.entities;

import jakarta.persistence.*;
import lombok.*;

//@Entity
//@Table(name = "study_levels", uniqueConstraints = {
//        @UniqueConstraint(columnNames = {"filiere_id", "order"})
//})
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class StudyLevel {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(nullable = false, length = 100)
//    private String label;
//
//    @Column(nullable = false, length = 10)
//    private String code;
//
//    @Column(name = "`order`", nullable = false)
//    private int order;
//
//    // Relation JPA — même base de données
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "filiere_id", nullable = false)
//    private Filiere filiere;
//
//    @Column(length = 20)
//    private String academicYear;
//}

@Entity
@Table(name = "study_levels", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"filiere_id", "level_order"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StudyLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(nullable = false, length = 10)
    private String code;

    @Column(name = "level_order", nullable = false)
    private int order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filiere_id", nullable = false)
    private Filiere filiere;

    @Column(length = 20)
    private String academicYear;
}