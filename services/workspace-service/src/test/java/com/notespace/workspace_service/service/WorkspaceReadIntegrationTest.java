package com.notespace.workspace_service.service;

import com.notespace.workspace_service.entity.*;
import com.notespace.workspace_service.repository.WorkspaceMemberRepository;
import com.notespace.workspace_service.exception.WorkspaceNotFoundException;
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
class WorkspaceReadIntegrationTest {
    @Autowired WorkspaceApplicationService service;
    @Autowired WorkspaceMemberRepository members;

    @Test
    void listsAndGetsOnlyAccessibleWorkspaces() {
        UUID owner = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        var shared = service.create("Shared", owner);
        var foreign = service.create("Private", UUID.randomUUID());
        members.saveAndFlush(WorkspaceMember.builder().workspace(shared).userId(member)
                .role(WorkspaceRole.MEMBER).joinedAt(Instant.now()).build());
        assertEquals(java.util.List.of(shared.getId()), service.listForUser(member).stream().map(Workspace::getId).toList());
        assertEquals(shared.getId(), service.getForUser(member, shared.getId()).getId());
        assertTrue(service.listForUser(UUID.randomUUID()).isEmpty());
        assertThrows(WorkspaceNotFoundException.class, () -> service.getForUser(member, foreign.getId()));
        assertThrows(WorkspaceNotFoundException.class, () -> service.getForUser(member, UUID.randomUUID()));
    }

    @Test
    void acceptsMaximumCanonicalNameAndRejectsEmptyAndUnicodeBlankNames() {
        assertEquals("a".repeat(100), service.create("  " + "a".repeat(100) + "  ", UUID.randomUUID()).getName());
        assertThrows(IllegalArgumentException.class, () -> service.create("", UUID.randomUUID()));
        assertThrows(IllegalArgumentException.class, () -> service.create("\u2003", UUID.randomUUID()));
    }
}
