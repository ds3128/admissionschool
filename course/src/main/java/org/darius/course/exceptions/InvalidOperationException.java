package org.darius.course.exceptions;

public class InvalidOperationException extends RuntimeException {
    public InvalidOperationException(String m){
        super(m);
    }
}