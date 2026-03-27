package org.darius.course.mapper;

import org.darius.course.dtos.responses.*;
import org.darius.course.entities.*;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface CourseMapper {

    SemesterResponse toSemesterResponse(Semester semester);

    @Mapping(target = "matieres", source = "matieres")
    TeachingUnitResponse toTeachingUnitResponse(TeachingUnit unit);

    @Mapping(target = "teachingUnitId",   source = "teachingUnit.id")
    @Mapping(target = "teachingUnitName", source = "teachingUnit.name")
    MatiereResponse toMatiereResponse(Matiere matiere);

    RoomResponse toRoomResponse(Room room);

    @Mapping(target = "currentSize", expression = "java(group.getStudentIds().size())")
    @Mapping(target = "semesterId",  source = "semester.id")
    StudentGroupResponse toStudentGroupResponse(StudentGroup group);

    @Mapping(target = "matiereId",     source = "matiere.id")
    @Mapping(target = "matiereName",   source = "matiere.name")
    @Mapping(target = "semesterId",    source = "semester.id")
    @Mapping(target = "semesterLabel", source = "semester.label")
    TeacherAssignmentResponse toTeacherAssignmentResponse(TeacherAssignment assignment);

    @Mapping(target = "matiereId",  source = "matiere.id")
    @Mapping(target = "matiereName",source = "matiere.name")
    @Mapping(target = "roomId",     source = "room.id")
    @Mapping(target = "roomName",   source = "room.name")
    @Mapping(target = "groupId",    source = "group.id")
    @Mapping(target = "groupName",  source = "group.name")
    @Mapping(target = "semesterId", source = "semester.id")
    PlannedSlotResponse toPlannedSlotResponse(PlannedSlot slot);

    @Mapping(target = "plannedSlotId", source = "plannedSlot.id")
    @Mapping(target = "matiereId",    source = "matiere.id")
    @Mapping(target = "matiereName",  source = "matiere.name")
    @Mapping(target = "roomId",       source = "room.id")
    @Mapping(target = "roomName",     source = "room.name")
    SessionResponse toSessionResponse(Session session);

    @Mapping(target = "sessionId", source = "session.id")
    AttendanceResponse toAttendanceResponse(Attendance attendance);

    @Mapping(target = "matiereId",        source = "matiere.id")
    @Mapping(target = "matiereName",      source = "matiere.name")
    @Mapping(target = "teachingUnitName", source = "matiere.teachingUnit.name")
    @Mapping(target = "groupId",          source = "group.id")
    @Mapping(target = "groupName",        source = "group.name")
    @Mapping(target = "semesterId",       source = "semester.id")
    EnrollmentResponse toEnrollmentResponse(Enrollment enrollment);

    @Mapping(target = "isPublished", source = "published")
    @Mapping(target = "matiereId",   source = "matiere.id")
    @Mapping(target = "matiereName", source = "matiere.name")
    @Mapping(target = "semesterId",  source = "semester.id")
    @Mapping(target = "attachments", source = "attachments")
    EvaluationResponse toEvaluationResponse(Evaluation evaluation);

    @Mapping(target = "evaluationId", source = "evaluation.id")
    EvaluationAttachmentResponse toAttachmentResponse(EvaluationAttachment attachment);

    @Mapping(target = "evaluationId",    source = "evaluation.id")
    @Mapping(target = "evaluationTitle", source = "evaluation.title")
    @Mapping(target = "matiereId",       source = "matiere.id")
    @Mapping(target = "matiereName",     source = "matiere.name")
    @Mapping(target = "maxScore",        source = "evaluation.maxScore")
    @Mapping(target = "coefficient",     source = "evaluation.coefficient")
    @Mapping(target = "scoreOn20",       expression = "java(grade.getScoreOn20())")
    GradeResponse toGradeResponse(Grade grade);

    @Mapping(target = "matiereId",   source = "matiere.id")
    @Mapping(target = "matiereName", source = "matiere.name")
    @Mapping(target = "semesterId",  source = "semester.id")
    CourseResourceResponse toCourseResourceResponse(CourseResource resource);

    @Mapping(target = "semesterId",    source = "semester.id")
    @Mapping(target = "semesterLabel", source = "semester.label")
    StudentProgressResponse toStudentProgressResponse(StudentProgress progress);
}
