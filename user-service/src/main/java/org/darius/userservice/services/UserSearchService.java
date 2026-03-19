package org.darius.userservice.services;

import org.darius.userservice.common.dtos.responses.PageResponse;
import org.darius.userservice.common.dtos.responses.SearchResultResponse;

public interface UserSearchService {

    /**
     * Recherche globale sur tous les types d'utilisateurs.
     * Recherche insensible à la casse sur firstName, lastName,
     * email, studentNumber, employeeNumber.
     * Minimum 2 caractères requis.
     */
    PageResponse<SearchResultResponse> search(String query, int page, int size);
}