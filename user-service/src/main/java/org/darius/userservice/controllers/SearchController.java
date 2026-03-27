package org.darius.userservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.darius.userservice.common.dtos.responses.PageResponse;
import org.darius.userservice.common.dtos.responses.SearchResultResponse;
import org.darius.userservice.services.UserSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/search")
@RequiredArgsConstructor
@Tag(name = "Recherche", description = "Recherche globale des utilisateurs")
public class SearchController {

    private final UserSearchService userSearchService;

    @GetMapping
    @Operation(summary = "Recherche globale - étudiants, enseignants, personnel")
    public ResponseEntity<PageResponse<SearchResultResponse>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(userSearchService.search(q, page, size));
    }
}