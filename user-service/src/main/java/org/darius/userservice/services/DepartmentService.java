package org.darius.userservice.services;

import org.darius.userservice.common.dtos.requests.AssignDepartmentHeadRequest;
import org.darius.userservice.common.dtos.requests.CreateDepartmentRequest;
import org.darius.userservice.common.dtos.requests.UpdateDepartmentRequest;
import org.darius.userservice.common.dtos.responses.DepartmentResponse;

import java.util.List;

public interface DepartmentService {

    /**
     * Crée un nouveau département.
     * Le code doit être unique et immuable.
     */
    DepartmentResponse createDepartment(CreateDepartmentRequest request);

    /**
     * Met à jour le nom et la description d'un département.
     * Le code est immuable.
     */
    DepartmentResponse updateDepartment(Long departmentId, UpdateDepartmentRequest request);

    /**
     * Retourne tous les départements.
     */
    List<DepartmentResponse> getAllDepartments();

    /**
     * Retourne un département par son ID.
     */
    DepartmentResponse getDepartmentById(Long departmentId);

    /**
     * Désigne un enseignant comme chef de département.
     * L'enseignant doit appartenir au département et être actif.
     */
    DepartmentResponse assignDepartmentHead(Long departmentId, AssignDepartmentHeadRequest request);
}