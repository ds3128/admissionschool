package org.darius.course.services;

import org.darius.course.dtos.requests.CreateMatiereRequest;
import org.darius.course.dtos.responses.MatiereResponse;
import java.util.List;

public interface MatiereService {

    MatiereResponse create(CreateMatiereRequest request);

    MatiereResponse update(Long id, CreateMatiereRequest request);

    MatiereResponse getById(Long id);

    List<MatiereResponse> getAll();

    List<MatiereResponse> getByTeachingUnit(Long teachingUnitId);

    List<MatiereResponse> getByStudyLevel(Long levelId);
}
