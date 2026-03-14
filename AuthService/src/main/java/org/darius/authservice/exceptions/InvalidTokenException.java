package org.darius.authservice.exceptions;

public class InvalidTokenException extends Throwable {
    public InvalidTokenException(String message) {
        super(message);
    }
}
