package org.darius.admission.repositories;

import org.darius.admission.common.enums.InterviewStatus;
import org.darius.admission.entities.Interview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    Optional<Interview> findByChoice_Id(Long choiceId);

    List<Interview> findByApplication_Id(String applicationId);

    List<Interview> findByStatus(InterviewStatus status);

    // Entretiens planifiés dans les prochaines 48h (pour rappels)
    @Query("""
        SELECT i FROM Interview i
        WHERE i.status = 'SCHEDULED'
          AND i.scheduledAt BETWEEN :now AND :in48h
        """)
    List<Interview> findUpcomingInterviews(
            @Param("now")    LocalDateTime now,
            @Param("in48h")  LocalDateTime in48h
    );
}