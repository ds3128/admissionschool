package org.darius.payment.services;

import org.darius.payment.common.dtos.responses.FinanceStatsResponse;

import java.util.List;

public interface FinanceStatsService {

    /** Statistiques financières globales. */
    FinanceStatsResponse getStats(String academicYear);

    /** Liste des étudiants bloqués pour impayé. */
    List<String> getBlockedStudentIds();
}