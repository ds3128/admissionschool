package org.darius.userservice.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "staff", indexes = {
        @Index(name = "idx_staff_number",  columnList = "staffNumber",  unique = true),
        @Index(name = "idx_staff_profile", columnList = "profileId",    unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Référence vers UserProfile.id (même service)
    @Column(nullable = false, unique = true)
    private String profileId;

    // Cross-service : référence vers Users.id dans l'Auth Service
    @Column(nullable = false, unique = true)
    private String userId;

    @Column(nullable = false, unique = true, length = 20)
    private String staffNumber;

    @Column(length = 150)
    private String position;

    // Relation JPA — Department est dans la même base de données
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(nullable = false)
    private boolean active = true;

    // Cross-service : référence vers l'admin ayant créé ce profil
    @Column(nullable = false)
    private String createdBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}