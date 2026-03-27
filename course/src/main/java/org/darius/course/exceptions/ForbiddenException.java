package org.darius.course.exceptions;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String m){
        super(m);
    }
}
