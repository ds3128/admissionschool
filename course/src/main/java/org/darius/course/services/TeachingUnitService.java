package org.darius.course.services;

import org.darius.course.dtos.requests.CreateTeachingUnitRequest;
import org.darius.course.dtos.responses.TeachingUnitResponse;
import java.util.List;

public interface TeachingUnitService {

    TeachingUnitResponse create(CreateTeachingUnitRequest request);

    TeachingUnitResponse update(Long id, CreateTeachingUnitRequest request);

    TeachingUnitResponse getById(Long id);

    List<TeachingUnitResponse> getAll();

    List<TeachingUnitResponse> getByStudyLevel(Long studyLevelId);
}