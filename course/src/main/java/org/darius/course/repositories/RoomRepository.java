package org.darius.course.repositories;

import org.darius.course.entities.Room;
import org.darius.course.enums.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByIsAvailable(boolean isAvailable);

    List<Room> findByType(RoomType type);

    List<Room> findByCapacityGreaterThanEqual(int minCapacity);

    // Salles disponibles sur un créneau — sans conflit de PlannedSlot
    @Query("""
        SELECT r FROM Room r
        WHERE r.isAvailable = true
          AND r.id NOT IN (
              SELECT ps.room.id FROM PlannedSlot ps
              WHERE ps.dayOfWeek = :dayOfWeek
                AND ps.startTime < :endTime
                AND ps.endTime > :startTime
                AND ps.semester.isCurrent = true
          )
        ORDER BY r.capacity
        """)
    List<Room> findAvailableOnSlot(
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );
}