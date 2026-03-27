package org.darius.notification.exceptions;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String m) {
        super(m);
    }
}