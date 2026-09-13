package com.notespace.userservice.exception;

public class InvalidCredentialsException extends RuntimeException{
    public InvalidCredentialsException() {
        super("Invalid password or email");
    }
}
