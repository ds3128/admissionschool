package org.darius.course.exceptions;

import org.darius.course.dtos.responses.ConflictResponse;

import java.util.List;

public class ConflictException extends RuntimeException {
    private final List<ConflictResponse> conflicts;
    public ConflictException(String message, List<ConflictResponse> conflicts) {
        super(message);
        this.conflicts = conflicts;
    }
    public List<ConflictResponse> getConflicts() {
        return conflicts;
    }
}