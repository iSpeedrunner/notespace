package com.notespace.workspace_service.exception;

public class WorkspaceNotFoundException extends RuntimeException {
    public WorkspaceNotFoundException() {
        super("Workspace not found");
    }
}
