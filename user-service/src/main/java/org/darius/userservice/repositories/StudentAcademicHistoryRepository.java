package org.darius.userservice.repositories;

import org.darius.userservice.entities.StudentAcademicHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentAcademicHistoryRepository extends JpaRepository<StudentAcademicHistory, Long> {

    // Historique complet trié par date décroissante
    List<StudentAcademicHistory> findByStudent_IdOrderByStartDateDesc(String studentId);

    // Enregistrement courant (endDate = null)
    Optional<StudentAcademicHistory> findByStudent_IdAndEndDateIsNull(String studentId);

    // Historique d'une filière donnée
    List<StudentAcademicHistory> findByStudent_IdAndFiliereId(String studentId, Long filiereId);
}