package com.notespace.workspace_service;

import com.notespace.workspace_service.entity.*;
import com.notespace.workspace_service.repository.*;
import com.notespace.workspace_service.service.WorkspaceApplicationService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** Explicit opt-in, disposable database only. Excluded from ordinary Surefire naming. */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "WORKSPACE_POSTGRES_TEST_URL", matches = ".+")
@Transactional
class WorkspacePostgresIT {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        String url = System.getenv("WORKSPACE_POSTGRES_TEST_URL");
        if (url == null || !url.matches("jdbc:postgresql://[^/]+/(workspace_mvp_test|workspace_smoke_[a-f0-9]+)")) {
            throw new IllegalArgumentException("A disposable workspace test database URL is required");
        }
        properties.add("spring.datasource.url", () -> url);
        properties.add("spring.datasource.username", () -> System.getenv("WORKSPACE_POSTGRES_TEST_USER"));
        properties.add("spring.datasource.password", () -> System.getenv().getOrDefault("WORKSPACE_POSTGRES_TEST_PASSWORD", ""));
        properties.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        properties.add("spring.flyway.enabled", () -> "true");
    }
    @Autowired WorkspaceApplicationService service;
    @Autowired WorkspaceRepository workspaces;
    @Autowired WorkspaceMemberRepository members;
    @Autowired WorkspaceInvitationRepository invitations;
    @Autowired EntityManager entities;
    @Autowired JdbcTemplate jdbc;

    @Test
    void flywayAndMappingsSupportRoundTripAndMembershipAuthorization() {
        assertEquals(1, jdbc.queryForObject("select count(*) from flyway_schema_history where success", Integer.class));
        UUID owner = UUID.randomUUID();
        var workspace = service.create("PostgreSQL", owner);
        var invitation = invitations.saveAndFlush(WorkspaceInvitation.builder().workspace(workspace)
                .inviteeUserId(UUID.randomUUID()).invitedByUserId(owner).status(WorkspaceInvitationStatus.PENDING)
                .createdAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600)).build());
        UUID id = invitation.getId();
        entities.clear();
        assertNotNull(id);
        assertEquals(WorkspaceInvitationStatus.PENDING, invitations.findById(id).orElseThrow().getStatus());
        assertEquals("PostgreSQL", service.getForUser(owner, workspace.getId()).getName());
        assertEquals(1, service.listForUser(owner).size());
        assertTrue(service.listForUser(UUID.randomUUID()).isEmpty());
        assertTrue(members.existsByWorkspace_IdAndUserId(workspace.getId(), owner));
    }

    @Test
    void duplicateMembershipIsRejectedByPostgres() {
        UUID owner = UUID.randomUUID();
        var workspace = service.create("Unique", owner);
        members.flush();
        assertThrows(DataIntegrityViolationException.class, () -> members.saveAndFlush(WorkspaceMember.builder()
                .workspace(workspace).userId(owner).role(WorkspaceRole.MEMBER).joinedAt(Instant.now()).build()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " padded ", "TOO_LONG"})
    void postgresRejectsInvalidStoredNames(String name) {
        String invalid = name.equals("TOO_LONG") ? "a".repeat(101) : name;
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "insert into workspace values (?, ?, current_timestamp, current_timestamp)", UUID.randomUUID(), invalid));
    }

    @Test
    void postgresRejectsUnsupportedMembershipRole() {
        var workspace = service.create("Roles", UUID.randomUUID());
        entities.flush();
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "insert into workspace_member values (?, ?, ?, 'ADMIN', current_timestamp)",
                UUID.randomUUID(), workspace.getId(), UUID.randomUUID()));
    }

    @Test
    void postgresRejectsUnsupportedInvitationStatus() {
        var workspace = service.create("Status", UUID.randomUUID());
        entities.flush();
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "insert into workspace_invitation values (?, ?, ?, ?, 'INVALID', current_timestamp, current_timestamp, null)",
                UUID.randomUUID(), workspace.getId(), UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void postgresRejectsOrphanMembership() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "insert into workspace_member values (?, ?, ?, 'MEMBER', current_timestamp)",
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void postgresRejectsOrphanInvitation() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "insert into workspace_invitation values (?, ?, ?, ?, 'PENDING', current_timestamp, current_timestamp, null)",
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));
    }
}
