package org.darius.course.services;

import org.darius.course.dtos.requests.CreateCourseResourceRequest;
import org.darius.course.dtos.requests.UpdateCourseResourceRequest;
import org.darius.course.dtos.responses.CourseResourceResponse;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface CourseResourceService {

    /**
     * Ajoute un support de cours (fichier ou lien externe).
     * Créé en DRAFT (isPublished = false).
     * L'enseignant doit être affecté à la matière.
     */
    CourseResourceResponse addResource(
            Long matiereId,
            String teacherId,
            CreateCourseResourceRequest request,
            MultipartFile file        // null si lien externe
    );

    CourseResourceResponse update(Long resourceId, String teacherId, UpdateCourseResourceRequest request);

    /**
     * Publie un support DRAFT → visible par les étudiants inscrits.
     * Seul l'auteur peut publier.
     */
    CourseResourceResponse publish(Long resourceId, String teacherId);

    /**
     * Dépublie un support → retour en DRAFT.
     * Seul l'auteur peut dépublier.
     */
    CourseResourceResponse unpublish(Long resourceId, String teacherId);

    /**
     * Soft delete — isDeleted = true.
     * Seul l'auteur peut supprimer.
     */
    void delete(Long resourceId, String teacherId);

    CourseResourceResponse getById(Long resourceId);

    /** Supports publiés d'une matière — vue étudiant. */
    List<CourseResourceResponse> getPublishedByMatiere(Long matiereId, String studentId);

    /** Tous les supports de l'enseignant (publiés + brouillons). */
    List<CourseResourceResponse> getMyResources(String teacherId);

    /** 3 derniers supports publiés — pour le CourseDashboard. */
    List<CourseResourceResponse> getRecentPublished(Long matiereId, int limit);
}