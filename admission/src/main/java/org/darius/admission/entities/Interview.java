package org.darius.admission.entities;

import jakarta.persistence.*;
import lombok.*;
import org.darius.admission.common.enums.InterviewStatus;
import org.darius.admission.common.enums.InterviewType;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "interviews")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "choice_id", nullable = false)
    private ApplicationChoice choice;

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    @Column(nullable = false)
    @Builder.Default
    private int duration = 30;

    @Column(length = 500)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InterviewType type = InterviewType.PRESENTIEL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InterviewStatus status = InterviewStatus.SCHEDULED;

    @ElementCollection
    @CollectionTable(name = "interview_interviewers",
            joinColumns = @JoinColumn(name = "interview_id"))
    @Column(name = "teacher_id")
    @Builder.Default
    private List<String> interviewers = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String notes;  // confidentiel — non visible par le candidat

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}