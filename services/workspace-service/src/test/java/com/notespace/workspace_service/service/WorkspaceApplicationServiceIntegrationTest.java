package com.notespace.workspace_service.service;

import com.notespace.workspace_service.entity.WorkspaceMember;
import com.notespace.workspace_service.entity.WorkspaceRole;
import com.notespace.workspace_service.repository.WorkspaceMemberRepository;
import com.notespace.workspace_service.repository.WorkspaceInvitationRepository;
import com.notespace.workspace_service.repository.WorkspaceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class WorkspaceApplicationServiceIntegrationTest {

    @Autowired
    private WorkspaceApplicationService workspaceApplicationService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private WorkspaceInvitationRepository workspaceInvitationRepository;

    @BeforeEach
    void clearDatabase() {
        workspaceInvitationRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
    }

    @AfterEach
    void clearDatabaseAfterTest() {
        clearDatabase();
    }

    @Test
    void createsNormalizedWorkspaceAndOwnerMembership() {
        UUID ownerUserId = UUID.randomUUID();

        var created = workspaceApplicationService.create("  Product Team  ", ownerUserId);

        assertNotNull(created.getId());
        assertEquals("Product Team", created.getName());
        assertNotNull(created.getCreatedAt());
        assertNotNull(created.getUpdatedAt());

        assertEquals(1, workspaceMemberRepository.count());
        WorkspaceMember member = workspaceMemberRepository.findAll().getFirst();
        assertEquals(created.getId(), member.getWorkspace().getId());
        assertEquals(ownerUserId, member.getUserId());
        assertEquals(WorkspaceRole.OWNER, member.getRole());
        assertNotNull(member.getJoinedAt());
    }

    @Test
    void rejectsWhitespaceOnlyWorkspaceNameWithoutPersistingAnything() {
        assertThrows(
                IllegalArgumentException.class,
                () -> workspaceApplicationService.create("   ", UUID.randomUUID())
        );

        assertEquals(0, workspaceRepository.count());
        assertEquals(0, workspaceMemberRepository.count());
    }

    @Test
    void rejectsNullWorkspaceNameWithoutPersistingAnything() {
        assertThrows(
                IllegalArgumentException.class,
                () -> workspaceApplicationService.create(null, UUID.randomUUID())
        );

        assertEquals(0, workspaceRepository.count());
        assertEquals(0, workspaceMemberRepository.count());
    }

    @Test
    void rejectsWorkspaceNameLongerThanOneHundredCharactersWithoutPersistingAnything() {
        assertThrows(
                IllegalArgumentException.class,
                () -> workspaceApplicationService.create("a".repeat(101), UUID.randomUUID())
        );

        assertEquals(0, workspaceRepository.count());
        assertEquals(0, workspaceMemberRepository.count());
    }

    @Test
    void rejectsNullOwnerUserIdWithoutPersistingAnything() {
        assertThrows(
                IllegalArgumentException.class,
                () -> workspaceApplicationService.create("Product Team", null)
        );

        assertEquals(0, workspaceRepository.count());
        assertEquals(0, workspaceMemberRepository.count());
    }
}
