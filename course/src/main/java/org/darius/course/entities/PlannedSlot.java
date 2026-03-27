package org.darius.course.entities;

import jakarta.persistence.*;
import lombok.*;
import org.darius.course.enums.SessionType;
import org.hibernate.annotations.CreationTimestamp;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "planned_slots",
        indexes = {
                @Index(name = "idx_slot_semester",  columnList = "semester_id"),
                @Index(name = "idx_slot_teacher",   columnList = "teacher_id"),
                @Index(name = "idx_slot_room",      columnList = "room_id"),
                @Index(name = "idx_slot_group",     columnList = "group_id")
        })
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PlannedSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matiere_id", nullable = false)
    private Matiere matiere;

    // Référence vers Teacher dans le User Service
    @Column(name = "teacher_id", nullable = false)
    private String teacherId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private StudentGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 15)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private SessionType type;

    @Column(nullable = false)
    @Builder.Default
    private boolean recurrent = true;

    @OneToMany(mappedBy = "plannedSlot", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Session> sessions = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}