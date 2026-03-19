package org.darius.userservice.services;

import org.darius.userservice.common.dtos.requests.TransferStudentRequest;
import org.darius.userservice.common.dtos.requests.UpdateStudentStatusRequest;
import org.darius.userservice.common.dtos.responses.PageResponse;
import org.darius.userservice.common.dtos.responses.StudentAcademicHistoryResponse;
import org.darius.userservice.common.dtos.responses.StudentResponse;
import org.darius.userservice.common.dtos.responses.StudentSummaryResponse;
import org.darius.userservice.common.enums.StudentStatus;
import org.darius.userservice.events.consumes.ApplicationAcceptedEvent;

import java.util.List;

public interface StudentService {

    /**
     * Crée un dossier étudiant complet à la réception de ApplicationAccepted.
     * Appelé par le consumer Kafka.
     */
    void createStudentFromAdmission(ApplicationAcceptedEvent event);

    /**
     * Retourne le dossier complet d'un étudiant par son ID.
     */
    StudentResponse getStudentById(String studentId);

    /**
     * Retourne le dossier d'un étudiant par son numéro matricule.
     */
    StudentResponse getStudentByNumber(String studentNumber);

    /**
     * Retourne le dossier de l'étudiant connecté.
     */
    StudentResponse getMyStudentProfile(String userEmail);

    /**
     * Liste paginée des étudiants avec filtres.
     */
    PageResponse<StudentSummaryResponse> getStudents(
            Long filiereId,
            Long levelId,
            StudentStatus status,
            int page,
            int size
    );

    /**
     * Promeut un étudiant au niveau supérieur.
     * Crée un enregistrement dans StudentAcademicHistory (reason = PROMOTION).
     * Publie StudentPromoted.
     */
    StudentResponse promoteStudent(String studentId);

    /**
     * Marque un étudiant comme diplômé.
     * Publie StudentGraduated.
     */
    StudentResponse graduateStudent(String studentId);

    /**
     * Transfère un étudiant vers une autre filière ou un autre niveau.
     * Crée un enregistrement dans StudentAcademicHistory.
     * Publie StudentTransferred.
     */
    StudentResponse transferStudent(String studentId, TransferStudentRequest request);

    /**
     * Change le statut académique d'un étudiant (SUSPENDED, EXPELLED, ON_LEAVE...).
     */
    StudentResponse updateStudentStatus(String studentId, UpdateStudentStatusRequest request);

    /**
     * Retourne l'historique académique complet d'un étudiant.
     */
    List<StudentAcademicHistoryResponse> getAcademicHistory(String studentId);
}