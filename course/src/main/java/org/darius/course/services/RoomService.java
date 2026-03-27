package org.darius.course.services;

import org.darius.course.dtos.requests.CreateRoomRequest;
import org.darius.course.dtos.responses.RoomResponse;
import org.darius.course.enums.RoomType;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public interface RoomService {

    RoomResponse create(CreateRoomRequest request);

    RoomResponse update(Long id, CreateRoomRequest request);

    RoomResponse getById(Long id);

    List<RoomResponse> getAll();

    List<RoomResponse> getByType(RoomType type);

    /** Salles disponibles sur un créneau horaire donné. */
    List<RoomResponse> getAvailable(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime);

    RoomResponse setAvailability(Long id, boolean available);
}
