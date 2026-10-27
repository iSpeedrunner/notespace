package com.notespace.workspace_service.service;

import com.notespace.workspace_service.entity.Workspace;
import com.notespace.workspace_service.entity.WorkspaceMember;
import com.notespace.workspace_service.entity.WorkspaceRole;
import com.notespace.workspace_service.repository.WorkspaceMemberRepository;
import com.notespace.workspace_service.repository.WorkspaceRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.List;
import com.notespace.workspace_service.exception.WorkspaceNotFoundException;

@Service
@AllArgsConstructor
public class WorkspaceApplicationService {
    private static final int MAX_NAME_LENGTH = 100;

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    @Transactional
    public Workspace create(String name, UUID ownerUserId) {
        String normalizedName = normalizeName(name);

        if (ownerUserId == null) {
            throw new IllegalArgumentException("Owner user ID must not be null");
        }

        Instant now = Instant.now();
        Workspace workspace = workspaceRepository.save(Workspace.builder()
                .name(normalizedName)
                .createdAt(now)
                .updatedAt(now)
                .build());

        workspaceMemberRepository.save(WorkspaceMember.builder()
                .workspace(workspace)
                .userId(ownerUserId)
                .role(WorkspaceRole.OWNER)
                .joinedAt(now)
                .build());

        return workspace;
    }

    @Transactional(readOnly = true)
    public List<Workspace> listForUser(UUID userId) {
        requireUserId(userId);
        return workspaceRepository.findAllForUser(userId);
    }

    @Transactional(readOnly = true)
    public Workspace getForUser(UUID userId, UUID workspaceId) {
        requireUserId(userId);
        if (workspaceId == null) {
            throw new IllegalArgumentException("Workspace ID must not be null");
        }
        return workspaceRepository.findForUser(workspaceId, userId)
                .orElseThrow(WorkspaceNotFoundException::new);
    }

    private void requireUserId(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
    }

    private String normalizeName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Workspace name must not be null");
        }

        String normalizedName = name.strip();
        if (normalizedName.isEmpty() || normalizedName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Workspace name must contain between 1 and 100 characters");
        }

        return normalizedName;
    }
}
