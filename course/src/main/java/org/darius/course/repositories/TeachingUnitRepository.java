package org.darius.course.repositories;

import org.darius.course.entities.TeachingUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeachingUnitRepository extends JpaRepository<TeachingUnit, Long> {

    Optional<TeachingUnit> findByCode(String code);

    boolean existsByCode(String code);

    List<TeachingUnit> findByStudyLevelId(Long studyLevelId);

    List<TeachingUnit> findByStudyLevelIdAndSemesterNumber(Long studyLevelId, int semesterNumber);
}
