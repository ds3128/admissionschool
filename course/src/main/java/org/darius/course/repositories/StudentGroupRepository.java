package org.darius.course.repositories;

import org.darius.course.entities.StudentGroup;
import org.darius.course.enums.GroupType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long> {

    List<StudentGroup> findBySemester_Id(Long semesterId);

    List<StudentGroup> findBySemester_IdAndFiliereId(Long semesterId, Long filiereId);

    List<StudentGroup> findBySemester_IdAndType(Long semesterId, GroupType type);

    Optional<StudentGroup> findBySemester_IdAndFiliereIdAndLevelIdAndType(
            Long semesterId, Long filiereId, Long levelId, GroupType type
    );

    // Groupes auxquels appartient un étudiant pour un semestre
    @Query("""
        SELECT sg FROM StudentGroup sg
        JOIN sg.studentIds si
        WHERE si = :studentId
          AND sg.semester.id = :semesterId
        """)
    List<StudentGroup> findByStudentIdAndSemesterId(
            @Param("studentId") String studentId,
            @Param("semesterId") Long semesterId
    );
}