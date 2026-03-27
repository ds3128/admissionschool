package org.darius.course.services;

import org.darius.course.dtos.requests.CreateStudentGroupRequest;
import org.darius.course.dtos.requests.UpdateGroupStudentsRequest;
import org.darius.course.dtos.responses.StudentGroupResponse;
import org.darius.course.enums.GroupType;
import java.util.List;

public interface StudentGroupService {

    StudentGroupResponse create(CreateStudentGroupRequest request);

    StudentGroupResponse getById(Long id);

    List<StudentGroupResponse> getBySemester(Long semesterId);

    List<StudentGroupResponse> getBySemesterAndType(Long semesterId, GroupType type);

    /** Ajoute ou retire des étudiants dans le groupe. */
    StudentGroupResponse updateStudents(Long id, UpdateGroupStudentsRequest request);

    List<String> getStudentIds(Long groupId);

    /**
     * Trouve ou crée le groupe PROMO pour un niveau/filière/semestre.
     * Appelé lors de la réception de StudentProfileCreated.
     */
    StudentGroupResponse findOrCreatePromoGroup(Long levelId, Long filiereId, Long semesterId);
}
