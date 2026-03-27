package org.darius.course.entities;

import jakarta.persistence.*;
import lombok.*;
import org.darius.course.enums.RoomType;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rooms")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String building;

    @Column(nullable = false)
    private int capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomType type;

    @ElementCollection
    @CollectionTable(
            name = "room_equipment",
            joinColumns = @JoinColumn(name = "room_id")
    )
    @Column(name = "equipment")
    @Builder.Default
    private List<String> equipment = new ArrayList<>();

    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private boolean isAvailable = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}