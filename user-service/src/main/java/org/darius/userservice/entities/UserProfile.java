package org.darius.userservice.entities;

import jakarta.persistence.*;
import lombok.*;
import org.darius.userservice.common.enums.Gender;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

//@Entity
//@Table(name = "user_profiles", indexes = {
//        @Index(name = "idx_user_id", columnList = "userId", unique = true),
//        @Index(name = "idx_personal_email", columnList = "personalEmail")
//})
//@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
//public class UserProfile {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.UUID)
//    private String id;
//
//    // Cross-service : référence vers Users.id dans l'Auth Service
//    @Column(nullable = false, unique = true)
//    private String userId;
//
//    @Column(length = 100)
//    private String firstName;
//
//    @Column(length = 100)
//    private String lastName;
//
//    @Column(length = 20)
//    private String phone;
//
//    private LocalDate birthDate;
//
//    @Column(length = 100)
//    private String birthPlace;
//
//    @Column(length = 100)
//    private String nationality;
//
//    @Enumerated(EnumType.STRING)
//    private Gender gender;
//
//    @Column(length = 255)
//    private String avatarUrl;
//
//    // Email personnel — conservé pour communications hors système
//    @Column(length = 150)
//    private String personalEmail;
//
//    @CreationTimestamp
//    @Column(updatable = false)
//    private LocalDateTime createdAt;
//
//    @UpdateTimestamp
//    private LocalDateTime updatedAt;
//}

@Entity
@Table(name = "user_profiles", indexes = {
        @Index(name = "idx_user_id", columnList = "userId", unique = true),
        @Index(name = "idx_personal_email", columnList = "personalEmail")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Cross-service : référence vers Users.id dans l'Auth Service
    @Column(nullable = false, unique = true)
    private String userId;

    @Column(length = 100)
    private String firstName;

    @Column(length = 100)
    private String lastName;

    @Column(length = 20)
    private String phone;

    private LocalDate birthDate;

    @Column(length = 100)
    private String birthPlace;

    @Column(length = 100)
    private String nationality;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(length = 255)
    private String avatarUrl;

    // Email personnel — conservé pour communications hors système
    @Column(length = 150)
    private String personalEmail;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean isBlocked = false;

    @Column(length = 500)
    private String blockReason;

    private LocalDateTime blockedAt;
}