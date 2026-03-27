package org.darius.course.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.course.dtos.requests.CreateMatiereRequest;
import org.darius.course.dtos.responses.MatiereResponse;
import org.darius.course.entities.Matiere;
import org.darius.course.entities.TeachingUnit;
import org.darius.course.exceptions.*;
import org.darius.course.mapper.CourseMapper;
import org.darius.course.repositories.EnrollmentRepository;
import org.darius.course.repositories.MatiereRepository;
import org.darius.course.repositories.TeachingUnitRepository;
import org.darius.course.services.MatiereService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatiereServiceImpl implements MatiereService {

    private final MatiereRepository      matiereRepository;
    private final TeachingUnitRepository teachingUnitRepository;
    private final EnrollmentRepository   enrollmentRepository;
    private final CourseMapper           mapper;

    @Override
    @Transactional
    public MatiereResponse create(CreateMatiereRequest request) {
        if (matiereRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException(
                    "Une matière avec le code " + request.getCode() + " existe déjà"
            );
        }

        TeachingUnit unit = teachingUnitRepository.findById(request.getTeachingUnitId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "UE introuvable : id=" + request.getTeachingUnitId()
                ));

        int totalHours = request.getHoursCM() + request.getHoursTD() + request.getHoursTP();

        Matiere matiere = Matiere.builder()
                .code(request.getCode())
                .name(request.getName())
                .teachingUnit(unit)
                .departmentId(request.getDepartmentId())
                .coefficient(request.getCoefficient())
                .hoursCM(request.getHoursCM())
                .hoursTD(request.getHoursTD())
                .hoursTP(request.getHoursTP())
                .totalHours(totalHours)
                .attendanceThreshold(request.getAttendanceThreshold())
                .build();

        return mapper.toMatiereResponse(matiereRepository.save(matiere));
    }

    @Override
    @Transactional
    public MatiereResponse update(Long id, CreateMatiereRequest request) {
        Matiere matiere = findOrThrow(id);

        if (!matiere.getCode().equals(request.getCode())
                && matiereRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException(
                    "Une matière avec le code " + request.getCode() + " existe déjà"
            );
        }

        TeachingUnit unit = teachingUnitRepository.findById(request.getTeachingUnitId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "UE introuvable : id=" + request.getTeachingUnitId()
                ));

        matiere.setCode(request.getCode());
        matiere.setName(request.getName());
        matiere.setTeachingUnit(unit);
        matiere.setDepartmentId(request.getDepartmentId());
        matiere.setCoefficient(request.getCoefficient());
        matiere.setHoursCM(request.getHoursCM());
        matiere.setHoursTD(request.getHoursTD());
        matiere.setHoursTP(request.getHoursTP());
        matiere.setTotalHours(request.getHoursCM() + request.getHoursTD() + request.getHoursTP());
        matiere.setAttendanceThreshold(request.getAttendanceThreshold());

        return mapper.toMatiereResponse(matiereRepository.save(matiere));
    }

    @Override
    @Transactional(readOnly = true)
    public MatiereResponse getById(Long id) {
        return mapper.toMatiereResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatiereResponse> getAll() {
        return matiereRepository.findAll().stream()
                .map(mapper::toMatiereResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatiereResponse> getByTeachingUnit(Long teachingUnitId) {
        return matiereRepository.findByTeachingUnit_Id(teachingUnitId).stream()
                .map(mapper::toMatiereResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatiereResponse> getByStudyLevel(Long levelId) {
        return matiereRepository.findByStudyLevelId(levelId).stream()
                .map(mapper::toMatiereResponse)
                .toList();
    }

    private Matiere findOrThrow(Long id) {
        return matiereRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Matière introuvable : id=" + id));
    }
}
