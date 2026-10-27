package com.notespace.workspace_service.repository;

import com.notespace.workspace_service.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WorkspaceMembershipQueryTest {
    @Autowired WorkspaceRepository workspaces;
    @Autowired WorkspaceMemberRepository members;
    @Autowired WorkspaceInvitationRepository invitations;

    @Test
    void queriesOnlyWorkspacesWithMembershipIncludingSharedWorkspaces() {
        UUID user = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        Workspace first = workspace();
        Workspace second = workspace();
        Workspace foreign = workspace();
        member(first, user); member(first, other); member(second, user); member(foreign, other);
        assertEquals(java.util.Set.of(first.getId(), second.getId()), workspaces.findAllForUser(user)
                .stream().map(Workspace::getId).collect(java.util.stream.Collectors.toSet()));
        assertTrue(workspaces.findAllForUser(UUID.randomUUID()).isEmpty());
        assertTrue(workspaces.findForUser(first.getId(), user).isPresent());
        assertTrue(workspaces.findForUser(foreign.getId(), user).isEmpty());
        assertTrue(workspaces.findForUser(UUID.randomUUID(), user).isEmpty());
        assertTrue(members.existsByWorkspace_IdAndUserId(first.getId(), user));
        assertFalse(members.existsByWorkspace_IdAndUserId(foreign.getId(), user));
    }

    @Test
    void invitationGeneratesUuidAndRoundTripsRequiredFields() {
        Workspace workspace = workspace();
        WorkspaceInvitation invitation = invitations.saveAndFlush(WorkspaceInvitation.builder()
                .workspace(workspace).inviteeUserId(UUID.randomUUID()).invitedByUserId(UUID.randomUUID())
                .status(WorkspaceInvitationStatus.PENDING).createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600)).build());
        assertNotNull(invitation.getId());
        assertEquals(workspace.getId(), invitations.findById(invitation.getId()).orElseThrow().getWorkspace().getId());
    }

    private Workspace workspace() {
        return workspaces.saveAndFlush(Workspace.builder().name("Team")
                .createdAt(Instant.now()).updatedAt(Instant.now()).build());
    }
    private void member(Workspace workspace, UUID user) {
        members.saveAndFlush(WorkspaceMember.builder().workspace(workspace).userId(user)
                .role(WorkspaceRole.MEMBER).joinedAt(Instant.now()).build());
    }
}
