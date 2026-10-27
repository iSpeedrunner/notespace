package com.notespace.workspace_service.dto;

import com.notespace.workspace_service.entity.Workspace;
import java.time.Instant;
import java.util.UUID;

public record WorkspaceResponse(UUID id, String name, Instant createdAt, Instant updatedAt) {
    public static WorkspaceResponse from(Workspace workspace) {
        return new WorkspaceResponse(workspace.getId(), workspace.getName(), workspace.getCreatedAt(), workspace.getUpdatedAt());
    }
}
