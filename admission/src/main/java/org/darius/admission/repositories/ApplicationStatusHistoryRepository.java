package org.darius.admission.repositories;

import org.darius.admission.entities.ApplicationStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationStatusHistoryRepository
        extends JpaRepository<ApplicationStatusHistory, Long> {

    List<ApplicationStatusHistory> findByApplication_IdOrderByChangedAtDesc(
            String applicationId
    );
}