package org.darius.course.controller.advices;

import lombok.extern.slf4j.Slf4j;
import org.darius.course.exceptions.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice @Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        p.setType(URI.create("/errors/not-found"));
        p.setTitle("Ressource introuvable");
        return p;
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicate(DuplicateResourceException ex) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        p.setType(URI.create("/errors/conflict"));
        p.setTitle("Ressource dupliquée");
        return p;
    }

    @ExceptionHandler(InvalidOperationException.class)
    public ProblemDetail handleInvalid(InvalidOperationException ex) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        p.setType(URI.create("/errors/invalid-operation"));
        p.setTitle("Opération invalide");
        return p;
    }

    @ExceptionHandler(ForbiddenException.class)
    public ProblemDetail handleForbidden(ForbiddenException ex) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        p.setType(URI.create("/errors/forbidden"));
        p.setTitle("Accès refusé");
        return p;
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException ex) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        p.setType(URI.create("/errors/planning-conflict"));
        p.setTitle("Conflit de planning");
        p.setProperty("conflicts", ex.getConflicts());
        return p;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(e ->
                errors.put(((FieldError) e).getField(), e.getDefaultMessage())
        );
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Erreurs de validation");
        p.setType(URI.create("/errors/validation"));
        p.setTitle("Erreurs de validation");
        p.setProperty("errors", errors);
        return p;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("Erreur inattendue : {}", ex.getMessage(), ex);
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne");
        p.setType(URI.create("/errors/internal"));
        p.setTitle("Erreur interne");
        return p;
    }
}
