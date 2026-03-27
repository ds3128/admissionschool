package org.darius.course.entities;

import jakarta.persistence.*;
import lombok.*;
import org.darius.course.enums.SessionStatus;
import org.darius.course.enums.SessionType;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sessions",
        indexes = {
                @Index(name = "idx_session_date",    columnList = "date"),
                @Index(name = "idx_session_matiere", columnList = "matiere_id"),
                @Index(name = "idx_session_status",  columnList = "status")
        })
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planned_slot_id")
    private PlannedSlot plannedSlot;

    // Enseignant effectif — peut différer du PlannedSlot
    @Column(name = "teacher_id", nullable = false)
    private String teacherId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matiere_id", nullable = false)
    private Matiere matiere;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    // Durée en minutes
    @Column(nullable = false)
    @Builder.Default
    private int duration = 90;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private SessionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SessionStatus status = SessionStatus.SCHEDULED;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Attendance> attendances = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}