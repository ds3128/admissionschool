package org.darius.course.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.course.dtos.requests.CreatePlannedSlotRequest;
import org.darius.course.dtos.responses.*;
import org.darius.course.entities.*;
import org.darius.course.enums.*;
import org.darius.course.exceptions.*;
import org.darius.course.mapper.CourseMapper;
import org.darius.course.repositories.*;
import org.darius.course.services.PlannedSlotService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;

@Service @RequiredArgsConstructor @Slf4j
public class PlannedSlotServiceImpl implements PlannedSlotService {

    private final PlannedSlotRepository  plannedSlotRepository;
    private final MatiereRepository      matiereRepository;
    private final RoomRepository         roomRepository;
    private final StudentGroupRepository groupRepository;
    private final SemesterRepository     semesterRepository;
    private final SessionRepository      sessionRepository;
    private final CourseMapper           mapper;

    @Override @Transactional
    public PlannedSlotResponse create(CreatePlannedSlotRequest req) {
        List<ConflictResponse> conflicts = detectConflicts(req, null);
        if (!conflicts.isEmpty()) throw new ConflictException("Conflit de planning", conflicts);

        Matiere      mat  = matiereRepository.findById(req.getMatiereId())
                .orElseThrow(() -> new ResourceNotFoundException("Matière introuvable"));
        Room         room = roomRepository.findById(req.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Salle introuvable"));
        StudentGroup grp  = groupRepository.findById(req.getGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Groupe introuvable"));
        Semester     sem  = semesterRepository.findById(req.getSemesterId())
                .orElseThrow(() -> new ResourceNotFoundException("Semestre introuvable"));

        if (room.getCapacity() < grp.getMaxSize())
            throw new InvalidOperationException(
                    "Salle " + room.getName() + " (cap=" + room.getCapacity()
                            + ") trop petite pour le groupe (max=" + grp.getMaxSize() + ")");

        PlannedSlot slot = PlannedSlot.builder()
                .matiere(mat).teacherId(req.getTeacherId()).room(room).group(grp).semester(sem)
                .dayOfWeek(req.getDayOfWeek()).startTime(req.getStartTime()).endTime(req.getEndTime())
                .type(req.getType()).recurrent(req.isRecurrent()).sessions(new ArrayList<>())
                .build();
        slot = plannedSlotRepository.save(slot);
        generateSessions(slot, sem);
        return mapper.toPlannedSlotResponse(slot);
    }

    @Override @Transactional
    public PlannedSlotResponse update(Long id, CreatePlannedSlotRequest req) {
        PlannedSlot slot = findOrThrow(id);
        List<ConflictResponse> conflicts = detectConflicts(req, id);
        if (!conflicts.isEmpty()) throw new ConflictException("Conflit lors de la modification", conflicts);

        Room room = roomRepository.findById(req.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Salle introuvable"));
        StudentGroup grp = groupRepository.findById(req.getGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Groupe introuvable"));
        if (room.getCapacity() < grp.getMaxSize())
            throw new InvalidOperationException("Salle trop petite pour le groupe");

        slot.setTeacherId(req.getTeacherId()); slot.setRoom(room); slot.setGroup(grp);
        slot.setDayOfWeek(req.getDayOfWeek()); slot.setStartTime(req.getStartTime());
        slot.setEndTime(req.getEndTime()); slot.setType(req.getType());
        return mapper.toPlannedSlotResponse(plannedSlotRepository.save(slot));
    }

    @Override @Transactional
    public void delete(Long id) {
        PlannedSlot slot = findOrThrow(id);
        slot.getSessions().stream()
                .filter(s -> s.getStatus() == SessionStatus.SCHEDULED && s.getDate().isAfter(LocalDate.now()))
                .forEach(s -> { s.setStatus(SessionStatus.CANCELLED); s.setCancelReason("Créneau supprimé"); sessionRepository.save(s); });
        plannedSlotRepository.delete(slot);
    }

    @Override @Transactional(readOnly = true)
    public PlannedSlotResponse getById(Long id) { return mapper.toPlannedSlotResponse(findOrThrow(id)); }

    @Override @Transactional(readOnly = true)
    public List<PlannedSlotResponse> getBySemester(Long sid) {
        return plannedSlotRepository.findBySemester_Id(sid).stream().map(mapper::toPlannedSlotResponse).toList(); }

    @Override @Transactional(readOnly = true)
    public List<PlannedSlotResponse> getByGroupAndSemester(Long gid, Long sid) {
        return plannedSlotRepository.findByGroup_IdAndSemester_Id(gid, sid).stream().map(mapper::toPlannedSlotResponse).toList(); }

    @Override @Transactional(readOnly = true)
    public List<PlannedSlotResponse> getByTeacherAndSemester(String tid, Long sid) {
        return plannedSlotRepository.findByTeacherIdAndSemester_Id(tid, sid).stream().map(mapper::toPlannedSlotResponse).toList(); }

    @Override @Transactional(readOnly = true)
    public List<ConflictResponse> detectConflicts(CreatePlannedSlotRequest req, Long excludeId) {
        List<ConflictResponse> list = new ArrayList<>();
        plannedSlotRepository.findRoomConflicts(req.getRoomId(), req.getDayOfWeek(), req.getStartTime(), req.getEndTime(), req.getSemesterId(), excludeId)
                .stream().findFirst().ifPresent(c -> list.add(ConflictResponse.builder().type(ConflictType.ROOM)
                        .message("Salle occupée le " + req.getDayOfWeek() + " " + c.getStartTime() + "→" + c.getEndTime()).conflictingSlotId(c.getId()).build()));
        plannedSlotRepository.findTeacherConflicts(req.getTeacherId(), req.getDayOfWeek(), req.getStartTime(), req.getEndTime(), req.getSemesterId(), excludeId)
                .stream().findFirst().ifPresent(c -> list.add(ConflictResponse.builder().type(ConflictType.TEACHER)
                        .message("Enseignant occupé le " + req.getDayOfWeek() + " " + c.getStartTime() + "→" + c.getEndTime()).conflictingSlotId(c.getId()).build()));
        plannedSlotRepository.findGroupConflicts(req.getGroupId(), req.getDayOfWeek(), req.getStartTime(), req.getEndTime(), req.getSemesterId(), excludeId)
                .stream().findFirst().ifPresent(c -> list.add(ConflictResponse.builder().type(ConflictType.GROUP)
                        .message("Groupe occupé le " + req.getDayOfWeek() + " " + c.getStartTime() + "→" + c.getEndTime()).conflictingSlotId(c.getId()).build()));
        return list;
    }

    @Override @Transactional(readOnly = true)
    public WeeklyScheduleResponse getWeeklySchedule(String userId, String role, LocalDate weekDate) {
        LocalDate monday = weekDate.with(DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);
        int weekNum = weekDate.get(WeekFields.of(Locale.FRANCE).weekOfWeekBasedYear());
        List<SessionResponse> sessions;
        if ("STUDENT".equals(role)) {
            Semester cur = semesterRepository.findByIsCurrent(true).orElse(null);
            if (cur == null) return buildEmpty(weekNum, monday, sunday);
            sessions = groupRepository.findByStudentIdAndSemesterId(userId, cur.getId()).stream()
                    .flatMap(g -> sessionRepository.findByGroupAndWeek(g.getId(), monday, sunday).stream())
                    .distinct().map(mapper::toSessionResponse).toList();
        } else if ("TEACHER".equals(role)) {
            sessions = sessionRepository.findByTeacherAndWeek(userId, monday, sunday).stream().map(mapper::toSessionResponse).toList();
        } else {
            sessions = sessionRepository.findByWeek(monday, sunday).stream().map(mapper::toSessionResponse).toList();
        }
        return WeeklyScheduleResponse.builder().weekNumber(weekNum).startDate(monday).endDate(sunday).sessions(sessions).slots(List.of()).build();
    }

    private void generateSessions(PlannedSlot slot, Semester sem) {
        if (!slot.isRecurrent()) { createSession(slot, nextOccurrence(slot.getDayOfWeek(), sem.getStartDate())); return; }
        LocalDate cur = nextOccurrence(slot.getDayOfWeek(), sem.getStartDate());
        while (!cur.isAfter(sem.getEndDate())) { createSession(slot, cur); cur = cur.plusWeeks(1); }
    }

    private void createSession(PlannedSlot slot, LocalDate date) {
        int dur = (int) java.time.Duration.between(slot.getStartTime(), slot.getEndTime()).toMinutes();
        sessionRepository.save(Session.builder().plannedSlot(slot).teacherId(slot.getTeacherId())
                .matiere(slot.getMatiere()).room(slot.getRoom()).date(date).startTime(slot.getStartTime())
                .duration(dur).type(slot.getType()).status(SessionStatus.SCHEDULED).build());
    }

    private LocalDate nextOccurrence(DayOfWeek dow, LocalDate from) {
        LocalDate d = from; while (d.getDayOfWeek() != dow) d = d.plusDays(1); return d;
    }
    private WeeklyScheduleResponse buildEmpty(int w, LocalDate f, LocalDate t) {
        return WeeklyScheduleResponse.builder().weekNumber(w).startDate(f).endDate(t).sessions(List.of()).slots(List.of()).build();
    }
    private PlannedSlot findOrThrow(Long id) {
        return plannedSlotRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Créneau introuvable : id=" + id));
    }
}