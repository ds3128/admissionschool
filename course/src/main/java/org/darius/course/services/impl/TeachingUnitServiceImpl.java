package org.darius.course.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.course.dtos.requests.CreateTeachingUnitRequest;
import org.darius.course.dtos.responses.TeachingUnitResponse;
import org.darius.course.entities.TeachingUnit;
import org.darius.course.exceptions.*;
import org.darius.course.mapper.CourseMapper;
import org.darius.course.repositories.TeachingUnitRepository;
import org.darius.course.services.TeachingUnitService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeachingUnitServiceImpl implements TeachingUnitService {

    private final TeachingUnitRepository teachingUnitRepository;
    private final CourseMapper           mapper;

    @Override
    @Transactional
    public TeachingUnitResponse create(CreateTeachingUnitRequest request) {
        if (teachingUnitRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException(
                    "Une UE avec le code " + request.getCode() + " existe déjà"
            );
        }

        TeachingUnit unit = TeachingUnit.builder()
                .code(request.getCode())
                .name(request.getName())
                .credits(request.getCredits())
                .studyLevelId(request.getStudyLevelId())
                .semesterNumber(request.getSemesterNumber())
                .coefficient(request.getCoefficient())
                .build();

        return mapper.toTeachingUnitResponse(teachingUnitRepository.save(unit));
    }

    @Override
    @Transactional
    public TeachingUnitResponse update(Long id, CreateTeachingUnitRequest request) {
        TeachingUnit unit = findOrThrow(id);

        // Vérifier unicité du code si modifié
        if (!unit.getCode().equals(request.getCode())
                && teachingUnitRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException(
                    "Une UE avec le code " + request.getCode() + " existe déjà"
            );
        }

        unit.setCode(request.getCode());
        unit.setName(request.getName());
        unit.setCredits(request.getCredits());
        unit.setStudyLevelId(request.getStudyLevelId());
        unit.setSemesterNumber(request.getSemesterNumber());
        unit.setCoefficient(request.getCoefficient());

        return mapper.toTeachingUnitResponse(teachingUnitRepository.save(unit));
    }

    @Override
    @Transactional(readOnly = true)
    public TeachingUnitResponse getById(Long id) {
        return mapper.toTeachingUnitResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeachingUnitResponse> getAll() {
        return teachingUnitRepository.findAll().stream()
                .map(mapper::toTeachingUnitResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeachingUnitResponse> getByStudyLevel(Long studyLevelId) {
        return teachingUnitRepository.findByStudyLevelId(studyLevelId).stream()
                .map(mapper::toTeachingUnitResponse)
                .toList();
    }

    private TeachingUnit findOrThrow(Long id) {
        return teachingUnitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UE introuvable : id=" + id));
    }
}
