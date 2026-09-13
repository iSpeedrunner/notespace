package com.notespace.userservice.exception;

public class UsernameAlreadyExistsException extends RuntimeException{
    public UsernameAlreadyExistsException() {
        super("Username is already taken");
    }
}
