package org.darius.course.entities;

import jakarta.persistence.*;
import lombok.*;
import org.darius.course.enums.ResourceType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "course_resources",
        indexes = {
                @Index(name = "idx_resource_matiere",  columnList = "matiere_id"),
                @Index(name = "idx_resource_semester",  columnList = "semester_id"),
                @Index(name = "idx_resource_teacher",   columnList = "uploaded_by"),
                @Index(name = "idx_resource_published", columnList = "is_published")
        })
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CourseResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matiere_id", nullable = false)
    private Matiere matiere;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ResourceType type = ResourceType.COURS;

    // URL S3/MinIO ou lien externe
    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    // null si lien externe
    @Column(name = "file_name", length = 255)
    private String fileName;

    // null si lien externe
    @Column(name = "file_size")
    private Long fileSize;

    // null si lien externe
    @Column(name = "mime_type", length = 100)
    private String mimeType;

    // false = DRAFT, true = PUBLIÉ
    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private boolean isPublished = false;

    // Soft delete
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    // ID de l'enseignant auteur (référence User Service)
    @Column(name = "uploaded_by", nullable = false)
    private String uploadedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}