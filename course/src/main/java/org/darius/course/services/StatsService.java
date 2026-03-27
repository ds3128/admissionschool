package org.darius.course.services;

import org.darius.course.dtos.responses.AttendanceStatsResponse;
import org.darius.course.dtos.responses.ClassStatsResponse;
import java.util.List;
import java.util.Map;

public interface StatsService {

    /**
     * Stats de présence pour le pipeline ML.
     * Agrège les AttendanceStats par étudiant et par matière.
     */
    List<AttendanceStatsResponse> getAttendanceStatsForML(Long semesterId, Long filiereId);

    /**
     * Stats de notes pour le pipeline ML.
     * Agrège les ClassStats par évaluation.
     */
    List<ClassStatsResponse> getGradeStatsForML(Long semesterId, Long matiereId);

    /**
     * Synthèse de progression pour le pipeline ML.
     * Agrège les StudentProgress par groupe/promo.
     * Calcule promoAverage, promoStdDev, passRate.
     */
    Map<String, Object> getProgressSummaryForML(Long semesterId, Long groupId);
}