package org.darius.course.services;

import org.darius.course.dtos.requests.UpdateEnrollmentStatusRequest;
import org.darius.course.dtos.responses.CourseDashboardEntry;
import org.darius.course.dtos.responses.EnrollmentResponse;
import java.util.List;

public interface EnrollmentService {

    EnrollmentResponse getById(Long id);

    List<EnrollmentResponse> getByStudentAndSemester(String studentId, Long semesterId);

    List<EnrollmentResponse> getByMatiereAndSemester(Long matiereId, Long semesterId);

    EnrollmentResponse updateStatus(Long id, UpdateEnrollmentStatusRequest request);

    /**
     * Crée les Enrollment pour toutes les matières d'un niveau.
     * Appelé lors de la réception de StudentProfileCreated.
     */
    void createEnrollmentsForStudent(String studentId, Long levelId, Long semesterId);

    /**
     * Bloque tous les Enrollment actifs d'un étudiant.
     * Appelé lors de la réception de StudentPaymentBlocked.
     */
    void blockStudentEnrollments(String studentId, Long semesterId);

    /**
     * Tableau de bord étudiant — vue synthétique de tous ses cours.
     * Combine inscriptions + notes provisoires + présences + supports.
     */
    List<CourseDashboardEntry> getStudentDashboard(String studentId, Long semesterId);
}