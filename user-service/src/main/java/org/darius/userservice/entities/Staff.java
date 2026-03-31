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

    @Column(nullable = false, unique = true)
    private String profileId;

    @Column(nullable = false, unique = true)
    private String userId;

    @Column(nullable = false, unique = true, length = 20)
    private String staffNumber;

    @Column(length = 150)
    private String position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private String createdBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}