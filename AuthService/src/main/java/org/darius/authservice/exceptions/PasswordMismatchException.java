package org.darius.authservice.exceptions;

public class PasswordMismatchException extends Throwable {
    public PasswordMismatchException(String message) {
        super(message);
    }
}
