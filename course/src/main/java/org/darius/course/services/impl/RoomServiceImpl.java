package org.darius.course.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.course.dtos.requests.CreateRoomRequest;
import org.darius.course.dtos.responses.RoomResponse;
import org.darius.course.entities.Room;
import org.darius.course.enums.RoomType;
import org.darius.course.exceptions.*;
import org.darius.course.mapper.CourseMapper;
import org.darius.course.repositories.RoomRepository;
import org.darius.course.services.RoomService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final CourseMapper   mapper;

    @Override
    @Transactional
    public RoomResponse create(CreateRoomRequest request) {
        Room room = Room.builder()
                .name(request.getName())
                .building(request.getBuilding())
                .capacity(request.getCapacity())
                .type(request.getType())
                .equipment(request.getEquipment() != null
                        ? new ArrayList<>(request.getEquipment())
                        : new ArrayList<>())
                .isAvailable(true)
                .build();

        return mapper.toRoomResponse(roomRepository.save(room));
    }

    @Override
    @Transactional
    public RoomResponse update(Long id, CreateRoomRequest request) {
        Room room = findOrThrow(id);

        room.setName(request.getName());
        room.setBuilding(request.getBuilding());
        room.setCapacity(request.getCapacity());
        room.setType(request.getType());
        if (request.getEquipment() != null) {
            room.getEquipment().clear();
            room.getEquipment().addAll(request.getEquipment());
        }

        return mapper.toRoomResponse(roomRepository.save(room));
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponse getById(Long id) {
        return mapper.toRoomResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getAll() {
        return roomRepository.findAll().stream()
                .map(mapper::toRoomResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getByType(RoomType type) {
        return roomRepository.findByType(type).stream()
                .map(mapper::toRoomResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getAvailable(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        return roomRepository.findAvailableOnSlot(dayOfWeek, startTime, endTime).stream()
                .map(mapper::toRoomResponse)
                .toList();
    }

    @Override
    @Transactional
    public RoomResponse setAvailability(Long id, boolean available) {
        Room room = findOrThrow(id);
        room.setAvailable(available);
        log.info("Salle {} : disponibilité → {}", room.getName(), available);
        return mapper.toRoomResponse(roomRepository.save(room));
    }

    private Room findOrThrow(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salle introuvable : id=" + id));
    }
}