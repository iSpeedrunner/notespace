package com.notespace.workspace_service.repository;

import com.notespace.workspace_service.entity.Workspace;
import com.notespace.workspace_service.entity.WorkspaceMember;
import com.notespace.workspace_service.entity.WorkspaceRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class WorkspaceMemberRepositoryTest {

    @Autowired
    WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    WorkspaceRepository workspaceRepository;

    private Workspace createWorkspace() {
        return Workspace.builder()
                .name("new")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private WorkspaceMember createWorkSpaceMember(UUID userId, Workspace workspace, WorkspaceRole role) {
        return WorkspaceMember.builder()
                .userId(userId)
                .workspace(workspace)
                .role(role)
                .build();
    }

    @Test
    void shouldSaveWorkspaceMemberWithOwnerRole() {
        Workspace savedWorkspace = workspaceRepository.saveAndFlush(createWorkspace());

        WorkspaceMember member = createWorkSpaceMember(
                UUID.randomUUID(),
                savedWorkspace,
                WorkspaceRole.OWNER
        );

        WorkspaceMember memberSaved = workspaceMemberRepository.saveAndFlush(member);

        assertNotNull(memberSaved.getId());
        assertEquals(WorkspaceRole.OWNER, memberSaved.getRole());

    }

    @Test
    void shouldRejectDuplicateWorkspaceMembership() {
        UUID userId = UUID.randomUUID();

        Workspace workspace = workspaceRepository.saveAndFlush(createWorkspace());

        WorkspaceMember first = createWorkSpaceMember(userId, workspace, WorkspaceRole.OWNER);

        WorkspaceMember duplicate = createWorkSpaceMember(userId, workspace, WorkspaceRole.MEMBER);

        workspaceMemberRepository.saveAndFlush(first);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> workspaceMemberRepository.saveAndFlush(duplicate)
        );


    }

}
