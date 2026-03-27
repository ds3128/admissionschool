package org.darius.course.services;

import org.darius.course.dtos.responses.StudentProgressResponse;
import org.darius.course.dtos.responses.TranscriptResponse;
import java.util.List;

public interface StudentProgressService {

    StudentProgressResponse getByStudentAndSemester(String studentId, Long semesterId);

    List<StudentProgressResponse> getByStudentId(String studentId);

    List<StudentProgressResponse> getBySemester(Long semesterId);

    /**
     * Calcule la progression semestrielle d'un étudiant.
     * Note finale par matière = Σ (score/maxScore × 20) × coeff évaluation.
     * Moyenne UE = Σ (note × coeff) / Σ coefficients.
     * Crédits = Σ crédits UEs validées (moyenne >= 10).
     * Rang = position dans le groupe PROMO.
     */
    StudentProgressResponse computeForStudent(String studentId, Long semesterId);

    /**
     * Calcule la progression de tous les étudiants d'un semestre.
     * Appelé par SemesterService.computeProgress().
     */
    List<StudentProgressResponse> computeAllForSemester(Long semesterId);

    /**
     * Relevé de notes complet — Transcript DTO.
     * Transmis au Document Service pour génération du bulletin.
     */
    TranscriptResponse getTranscript(String studentId, String academicYear, Long semesterId);
}