package org.darius.admission.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dossiers")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Dossier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<DossierDocument> documents = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private boolean isComplete = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean isLocked = false;

    private LocalDateTime lockedAt;

    @Column(columnDefinition = "TEXT")
    private String unlockReason;
}