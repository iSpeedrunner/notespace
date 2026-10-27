package com.notespace.workspace_service.repository;

import com.notespace.workspace_service.entity.Workspace;
import com.notespace.workspace_service.entity.WorkspaceInvitation;
import com.notespace.workspace_service.entity.WorkspaceInvitationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
public class WorkspaceInvitationRepositoryTest {

    @Autowired
    private WorkspaceInvitationRepository workspaceInvitationRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private Workspace createWorkspace() {
        Instant now = Instant.now();

        return Workspace.builder()
                .name("Invitation test workspace")
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private WorkspaceInvitation createPendingInvitation(Workspace workspace) {
        Instant now = Instant.now();

        return WorkspaceInvitation.builder()
                .workspace(workspace)
                .inviteeUserId(UUID.randomUUID())
                .invitedByUserId(UUID.randomUUID())
                .status(WorkspaceInvitationStatus.PENDING)
                .expiresAt(now.plusSeconds(3600))
                .createdAt(now)
                .build();
    }

    @Test
    void shouldSavePendingWorkspaceInvitation() {
        Workspace workspace = workspaceRepository.saveAndFlush(createWorkspace());

        WorkspaceInvitation saved = workspaceInvitationRepository.saveAndFlush(
                createPendingInvitation(workspace)
        );

        assertNotNull(saved.getId());
        assertEquals(WorkspaceInvitationStatus.PENDING, saved.getStatus());
    }

    @Test
    void shouldFindWorkspaceInvitationById() {
        Workspace workspace = workspaceRepository.saveAndFlush(createWorkspace());
        WorkspaceInvitation saved = workspaceInvitationRepository.saveAndFlush(
                createPendingInvitation(workspace)
        );

        WorkspaceInvitation found = workspaceInvitationRepository.findById(saved.getId())
                .orElseThrow();

        assertEquals(saved.getId(), found.getId());
        assertEquals(WorkspaceInvitationStatus.PENDING, found.getStatus());
    }
}
