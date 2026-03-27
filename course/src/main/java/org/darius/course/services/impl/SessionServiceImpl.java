package org.darius.course.services.impl;

import lombok.RequiredArgsConstructor; import lombok.extern.slf4j.Slf4j;
import org.darius.course.dtos.requests.*;
import org.darius.course.dtos.responses.*;
import org.darius.course.entities.*;
import org.darius.course.enums.SessionStatus;
import org.darius.course.events.published.SessionCancelledEvent;
import org.darius.course.exceptions.*;
import org.darius.course.kafka.CourseEventProducer;
import org.darius.course.mapper.CourseMapper;
import org.darius.course.repositories.*;
import org.darius.course.services.AttendanceService;
import org.darius.course.services.SessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor @Slf4j
public class SessionServiceImpl implements SessionService {

    private final SessionRepository    sessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final RoomRepository       roomRepository;
    private final AttendanceService    attendanceService;
    private final CourseEventProducer  eventProducer;
    private final CourseMapper         mapper;

    @Override @Transactional(readOnly = true)
    public SessionResponse getById(Long id) { return mapper.toSessionResponse(findOrThrow(id)); }

    @Override @Transactional(readOnly = true)
    public List<SessionResponse> getByMatiereAndSemester(Long mid, Long sid) {
        return sessionRepository.findByMatiere_IdAndPlannedSlot_Semester_Id(mid, sid).stream().map(mapper::toSessionResponse).toList();
    }

    @Override @Transactional
    public List<AttendanceResponse> markAttendance(Long sessionId, String teacherId, MarkAttendanceRequest req) {
        Session session = findOrThrow(sessionId);
        if (!session.getTeacherId().equals(teacherId))
            throw new ForbiddenException("Vous n'êtes pas l'enseignant de cette séance");

        List<AttendanceResponse> responses = new ArrayList<>();
        for (var entry : req.getEntries()) {
            Attendance att = attendanceRepository.findBySession_IdAndStudentId(sessionId, entry.getStudentId())
                    .orElse(Attendance.builder().session(session).studentId(entry.getStudentId()).build());
            att.setPresent(entry.isPresent());
            if (entry.getJustification() != null && !entry.isPresent()) att.setJustification(entry.getJustification());
            responses.add(mapper.toAttendanceResponse(attendanceRepository.save(att)));
        }

        session.setStatus(SessionStatus.DONE);
        sessionRepository.save(session);

        Long semesterId = session.getPlannedSlot() != null ? session.getPlannedSlot().getSemester().getId() : null;
        if (semesterId != null) {
            req.getEntries().stream().filter(e -> !e.isPresent())
                    .forEach(e -> attendanceService.checkThreshold(e.getStudentId(), session.getMatiere().getId(), semesterId));
        }
        return responses;
    }

    @Override @Transactional
    public SessionResponse cancel(Long sessionId, CancelSessionRequest req) {
        Session session = findOrThrow(sessionId);
        if (session.getStatus() == SessionStatus.DONE)
            throw new InvalidOperationException("Une séance DONE ne peut pas être annulée");
        session.setStatus(SessionStatus.CANCELLED); session.setCancelReason(req.getReason());
        sessionRepository.save(session);
        List<String> studentIds = session.getPlannedSlot() != null ? session.getPlannedSlot().getGroup().getStudentIds() : List.of();
        eventProducer.publishSessionCancelled(SessionCancelledEvent.builder()
                .sessionId(session.getId()).matiereId(session.getMatiere().getId())
                .matiereName(session.getMatiere().getName()).date(session.getDate())
                .startTime(session.getStartTime()).reason(req.getReason()).affectedStudentIds(studentIds).build());
        return mapper.toSessionResponse(session);
    }

    @Override @Transactional
    public SessionResponse reschedule(Long sessionId, RescheduleSessionRequest req) {
        Session orig = findOrThrow(sessionId);
        if (orig.getStatus() == SessionStatus.DONE)
            throw new InvalidOperationException("Une séance DONE ne peut pas être reportée");
        Room newRoom = req.getNewRoomId() != null
                ? roomRepository.findById(req.getNewRoomId()).orElseThrow(() -> new ResourceNotFoundException("Salle introuvable"))
                : orig.getRoom();
        orig.setStatus(SessionStatus.RESCHEDULED); orig.setCancelReason(req.getReason());
        sessionRepository.save(orig);
        int dur = (int) java.time.Duration.between(req.getNewStartTime(), req.getNewEndTime()).toMinutes();
        return mapper.toSessionResponse(sessionRepository.save(Session.builder()
                .plannedSlot(orig.getPlannedSlot()).teacherId(orig.getTeacherId())
                .matiere(orig.getMatiere()).room(newRoom).date(req.getNewDate())
                .startTime(req.getNewStartTime()).duration(dur).type(orig.getType()).status(SessionStatus.SCHEDULED).build()));
    }

    @Override @Transactional
    public void generateWeeklySessions() { log.info("Scheduler : vérification sessions semaine à venir"); }

    private Session findOrThrow(Long id) {
        return sessionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Session introuvable : id=" + id));
    }
}