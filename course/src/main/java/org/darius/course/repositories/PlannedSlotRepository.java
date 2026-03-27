package org.darius.course.repositories;

import org.darius.course.entities.PlannedSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public interface PlannedSlotRepository extends JpaRepository<PlannedSlot, Long> {

    List<PlannedSlot> findBySemester_Id(Long semesterId);

    List<PlannedSlot> findByGroup_IdAndSemester_Id(Long groupId, Long semesterId);

    List<PlannedSlot> findByTeacherIdAndSemester_Id(String teacherId, Long semesterId);

    List<PlannedSlot> findByMatiere_IdAndSemester_Id(Long matiereId, Long semesterId);

    // Détection conflit salle
    @Query("""
        SELECT ps FROM PlannedSlot ps
        WHERE ps.room.id = :roomId
          AND ps.dayOfWeek = :dayOfWeek
          AND ps.semester.id = :semesterId
          AND ps.startTime < :endTime
          AND ps.endTime > :startTime
          AND (:excludeId IS NULL OR ps.id <> :excludeId)
        """)
    List<PlannedSlot> findRoomConflicts(
            @Param("roomId") Long roomId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("semesterId") Long semesterId,
            @Param("excludeId") Long excludeId
    );

    // Détection conflit enseignant
    @Query("""
        SELECT ps FROM PlannedSlot ps
        WHERE ps.teacherId = :teacherId
          AND ps.dayOfWeek = :dayOfWeek
          AND ps.semester.id = :semesterId
          AND ps.startTime < :endTime
          AND ps.endTime > :startTime
          AND (:excludeId IS NULL OR ps.id <> :excludeId)
        """)
    List<PlannedSlot> findTeacherConflicts(
            @Param("teacherId") String teacherId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("semesterId") Long semesterId,
            @Param("excludeId") Long excludeId
    );

    // Détection conflit groupe
    @Query("""
        SELECT ps FROM PlannedSlot ps
        WHERE ps.group.id = :groupId
          AND ps.dayOfWeek = :dayOfWeek
          AND ps.semester.id = :semesterId
          AND ps.startTime < :endTime
          AND ps.endTime > :startTime
          AND (:excludeId IS NULL OR ps.id <> :excludeId)
        """)
    List<PlannedSlot> findGroupConflicts(
            @Param("groupId") Long groupId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("semesterId") Long semesterId,
            @Param("excludeId") Long excludeId
    );
}
