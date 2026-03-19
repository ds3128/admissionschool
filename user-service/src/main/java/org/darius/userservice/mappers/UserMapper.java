package org.darius.userservice.mappers;

import org.darius.userservice.common.dtos.requests.UpdateProfileRequest;
import org.darius.userservice.common.dtos.responses.*;
import org.darius.userservice.entities.*;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserMapper {

    // ── UserProfile ──────────────────────────────────────────────────────────

    UserProfileResponse toUserProfileResponse(UserProfile profile);


    // ── Department ───────────────────────────────────────────────────────────

    @Mapping(target = "teacherCount", expression = "java(department.getTeachers().size())")
    @Mapping(target = "filiereCount", expression = "java(department.getFilieres().size())")
    DepartmentResponse toDepartmentResponse(Department department);

    List<DepartmentResponse> toDepartmentResponseList(List<Department> departments);


    // ── StudyLevel ────────────────────────────────────────────────────────────

    @Mapping(target = "filiereId", source = "filiere.id")
    StudyLevelResponse toStudyLevelResponse(StudyLevel studyLevel);

    List<StudyLevelResponse> toStudyLevelResponseList(List<StudyLevel> studyLevels);


    // ── Filiere ───────────────────────────────────────────────────────────────

    @Mapping(target = "departmentId",   source = "department.id")
    @Mapping(target = "departmentName", source = "department.name")
    @Mapping(target = "studyLevels",    source = "studyLevels")
    FiliereResponse toFiliereResponse(Filiere filiere);

    List<FiliereResponse> toFiliereResponseList(List<Filiere> filieres);


    // ── StudentAcademicHistory ────────────────────────────────────────────────

    StudentAcademicHistoryResponse toHistoryResponse(StudentAcademicHistory history);

    List<StudentAcademicHistoryResponse> toHistoryResponseList(List<StudentAcademicHistory> histories);


    // ── Student ───────────────────────────────────────────────────────────────
    // Multi-source : on qualifie EXPLICITEMENT tous les champs ambigus

    @Mapping(target = "id",                    source = "student.id")
    @Mapping(target = "profileId",             source = "student.profileId")
    @Mapping(target = "studentNumber",         source = "student.studentNumber")
    @Mapping(target = "enrollmentYear",        source = "student.enrollmentYear")
    @Mapping(target = "status",                source = "student.status")
    @Mapping(target = "admissionApplicationId",source = "student.admissionApplicationId")
    @Mapping(target = "createdAt",             source = "student.createdAt")
    @Mapping(target = "filiereId",             source = "student.filiere.id")
    @Mapping(target = "filiereName",           source = "student.filiere.name")
    @Mapping(target = "currentLevelId",        source = "student.currentLevel.id")
    @Mapping(target = "currentLevelLabel",     source = "student.currentLevel.label")
    @Mapping(target = "academicHistory",       source = "student.academicHistory")
    @Mapping(target = "firstName",             source = "profile.firstName")
    @Mapping(target = "lastName",              source = "profile.lastName")
    @Mapping(target = "avatarUrl",             source = "profile.avatarUrl")
    StudentResponse toStudentResponse(Student student, UserProfile profile);

    @Mapping(target = "id",                source = "student.id")
    @Mapping(target = "studentNumber",     source = "student.studentNumber")
    @Mapping(target = "status",            source = "student.status")
    @Mapping(target = "filiereName",       source = "student.filiere.name")
    @Mapping(target = "currentLevelLabel", source = "student.currentLevel.label")
    @Mapping(target = "firstName",         source = "profile.firstName")
    @Mapping(target = "lastName",          source = "profile.lastName")
    @Mapping(target = "avatarUrl",         source = "profile.avatarUrl")
    StudentSummaryResponse toStudentSummaryResponse(Student student, UserProfile profile);


    // ── Teacher ───────────────────────────────────────────────────────────────
    // Multi-source : on qualifie EXPLICITEMENT tous les champs ambigus

    @Mapping(target = "id",              source = "teacher.id")
    @Mapping(target = "profileId",       source = "teacher.profileId")
    @Mapping(target = "userId",          source = "teacher.userId")
    @Mapping(target = "employeeNumber",  source = "teacher.employeeNumber")
    @Mapping(target = "speciality",      source = "teacher.speciality")
    @Mapping(target = "grade",           source = "teacher.grade")
    @Mapping(target = "diploma",         source = "teacher.diploma")
    @Mapping(target = "maxHoursPerWeek", source = "teacher.maxHoursPerWeek")
    @Mapping(target = "isActive",        source = "teacher.active")
    @Mapping(target = "createdAt",       source = "teacher.createdAt")
    @Mapping(target = "departmentId",    source = "teacher.department.id")
    @Mapping(target = "departmentName",  source = "teacher.department.name")
    @Mapping(target = "firstName",       source = "profile.firstName")
    @Mapping(target = "lastName",        source = "profile.lastName")
    @Mapping(target = "avatarUrl",       source = "profile.avatarUrl")
    @Mapping(target = "personalEmail",   source = "profile.personalEmail")
    TeacherResponse toTeacherResponse(Teacher teacher, UserProfile profile);

    @Mapping(target = "id",             source = "teacher.id")
    @Mapping(target = "employeeNumber", source = "teacher.employeeNumber")
    @Mapping(target = "speciality",     source = "teacher.speciality")
    @Mapping(target = "grade",          source = "teacher.grade")
    @Mapping(target = "isActive",       source = "teacher.active")
    @Mapping(target = "departmentName", source = "teacher.department.name")
    @Mapping(target = "firstName",      source = "profile.firstName")
    @Mapping(target = "lastName",       source = "profile.lastName")
    @Mapping(target = "avatarUrl",      source = "profile.avatarUrl")
    TeacherSummaryResponse toTeacherSummaryResponse(Teacher teacher, UserProfile profile);


    // ── Staff ─────────────────────────────────────────────────────────────────
    // Multi-source : on qualifie EXPLICITEMENT tous les champs ambigus

    @Mapping(target = "id",             source = "staff.id")
    @Mapping(target = "profileId",      source = "staff.profileId")
    @Mapping(target = "userId",         source = "staff.userId")
    @Mapping(target = "staffNumber",    source = "staff.staffNumber")
    @Mapping(target = "position",       source = "staff.position")
    @Mapping(target = "isActive",       source = "staff.active")
    @Mapping(target = "createdAt",      source = "staff.createdAt")
    @Mapping(target = "departmentId",   source = "staff.department.id")
    @Mapping(target = "departmentName", source = "staff.department.name")
    @Mapping(target = "firstName",      source = "profile.firstName")
    @Mapping(target = "lastName",       source = "profile.lastName")
    @Mapping(target = "avatarUrl",      source = "profile.avatarUrl")
    StaffResponse toStaffResponse(Staff staff, UserProfile profile);


    // ── Partial update ────────────────────────────────────────────────────────

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateProfileFromRequest(
            @MappingTarget UserProfile target,
            UpdateProfileRequest request
    );
}