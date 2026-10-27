package com.notespace.workspace_service.exception;

public class CurrentUserRequiredException extends RuntimeException {
    public CurrentUserRequiredException() {
        super("Trusted current user identity is required");
    }
}
