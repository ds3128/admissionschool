package org.darius.userservice.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.darius.userservice.common.dtos.requests.AssignDepartmentHeadRequest;
import org.darius.userservice.common.dtos.requests.CreateDepartmentRequest;
import org.darius.userservice.common.dtos.requests.UpdateDepartmentRequest;
import org.darius.userservice.common.dtos.responses.DepartmentResponse;
import org.darius.userservice.entities.Department;
import org.darius.userservice.entities.Teacher;
import org.darius.userservice.exceptions.DuplicateResourceException;
import org.darius.userservice.exceptions.InvalidOperationException;
import org.darius.userservice.exceptions.UserNotFoundException;
import org.darius.userservice.mappers.UserMapper;
import org.darius.userservice.repositories.DepartmentRepository;
import org.darius.userservice.repositories.TeacherRepository;
import org.darius.userservice.services.DepartmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Service
@Slf4j
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final TeacherRepository teacherRepository;
    private final UserMapper userMapper;

    public DepartmentServiceImpl(DepartmentRepository departmentRepository, TeacherRepository teacherRepository, UserMapper userMapper) {
        this.departmentRepository = departmentRepository;
        this.teacherRepository = teacherRepository;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public DepartmentResponse createDepartment(CreateDepartmentRequest request) {
        log.info("Création du département : code={}", request.getCode());

        if (departmentRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException(
                    "Un département avec le code '" + request.getCode() + "' existe déjà"
            );
        }

        Department department = Department.builder()
                .name(request.getName())
                .code(request.getCode().toUpperCase())
                .description(request.getDescription())
                .build();

        department = departmentRepository.save(department);
        log.info("Département créé : id={}, code={}", department.getId(), department.getCode());

        return userMapper.toDepartmentResponse(department);
    }

    @Override
    @Transactional
    public DepartmentResponse updateDepartment(Long departmentId, UpdateDepartmentRequest request) {
        Department department = findDepartmentOrThrow(departmentId);

        if (request.getName() != null) {
            department.setName(request.getName());
        }
        if (request.getDescription() != null) {
            department.setDescription(request.getDescription());
        }

        Department saved = departmentRepository.save(department);

        return userMapper.toDepartmentResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments() {
        return userMapper.toDepartmentResponseList(departmentRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(Long departmentId) {
        return userMapper.toDepartmentResponse(findDepartmentOrThrow(departmentId));
    }

    @Override
    @Transactional
    public DepartmentResponse assignDepartmentHead(Long departmentId, AssignDepartmentHeadRequest request) {
        Department department = findDepartmentOrThrow(departmentId);

        Teacher teacher = teacherRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new UserNotFoundException(
                        "Teacher not found : id=" + request.getTeacherId()
                ));

        // L'enseignant doit appartenir à ce département et être actif
        if (!teacher.getDepartment().getId().equals(departmentId)) {
            throw new InvalidOperationException(
                    "Teacher must be the department id=" + departmentId
            );
        }
        if (!teacher.isActive()) {
            throw new InvalidOperationException(
                    "Teacher cannot be assigned as a head of department because is inactive id=" + departmentId
            );
        }

        department.setHeadTeacherId(teacher.getId());
        log.info("Head of department assigned : departmentId={}, teacherId={}", departmentId, teacher.getId());

        return userMapper.toDepartmentResponse(departmentRepository.save(department));
    }

    private Department findDepartmentOrThrow(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new UserNotFoundException(
                        "Department not found : id=" + departmentId
                ));
    }
}
