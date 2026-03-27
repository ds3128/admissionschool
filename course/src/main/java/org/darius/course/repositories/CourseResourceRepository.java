package org.darius.course.repositories;

import org.darius.course.entities.CourseResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseResourceRepository extends JpaRepository<CourseResource, Long> {

    // Supports publiés d'une matière — vue étudiant
    @Query("""
        SELECT r FROM CourseResource r
        WHERE r.matiere.id = :matiereId
          AND r.isPublished = true
          AND r.isDeleted = false
        ORDER BY r.type, r.createdAt DESC
        """)
    List<CourseResource> findPublishedByMatiereId(@Param("matiereId") Long matiereId);

    // Tous les supports d'un enseignant pour une matière — vue enseignant
    @Query("""
        SELECT r FROM CourseResource r
        WHERE r.matiere.id = :matiereId
          AND r.uploadedBy = :teacherId
          AND r.isDeleted = false
        ORDER BY r.createdAt DESC
        """)
    List<CourseResource> findByMatiereIdAndTeacher(
            @Param("matiereId") Long matiereId,
            @Param("teacherId") String teacherId
    );

    // Tous les supports d'un enseignant toutes matières confondues
    @Query("""
        SELECT r FROM CourseResource r
        WHERE r.uploadedBy = :teacherId
          AND r.isDeleted = false
        ORDER BY r.createdAt DESC
        """)
    List<CourseResource> findByTeacherId(@Param("teacherId") String teacherId);

    // 3 derniers supports publiés d'une matière — pour le CourseDashboard
    @Query("""
        SELECT r FROM CourseResource r
        WHERE r.matiere.id = :matiereId
          AND r.isPublished = true
          AND r.isDeleted = false
        ORDER BY r.createdAt DESC
        LIMIT 3
        """)
    List<CourseResource> findTop3PublishedByMatiereId(@Param("matiereId") Long matiereId);

    // Nombre de supports publiés d'une matière
    long countByMatiere_IdAndIsPublishedTrueAndIsDeletedFalse(Long matiereId);

    // Supports publiés d'une matière pour un semestre donné
    List<CourseResource> findByMatiere_IdAndSemester_IdAndIsPublishedTrueAndIsDeletedFalse(
            Long matiereId, Long semesterId
    );
}