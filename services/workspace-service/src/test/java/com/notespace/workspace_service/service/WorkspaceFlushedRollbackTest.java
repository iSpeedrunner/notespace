package com.notespace.workspace_service.service;

import com.notespace.workspace_service.entity.WorkspaceMember;
import com.notespace.workspace_service.repository.WorkspaceMemberRepository;
import com.notespace.workspace_service.repository.WorkspaceRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:flushed_rollback;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class WorkspaceFlushedRollbackTest {
    @Autowired WorkspaceApplicationService service;
    @Autowired WorkspaceRepository workspaces;
    @Autowired EntityManager entities;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean WorkspaceMemberRepository members;

    @Test
    void rollsBackAnActuallyInsertedWorkspaceThroughSpringProxy() {
        assertTrue(AopUtils.isAopProxy(service));
        doAnswer(invocation -> {
            entities.flush();
            assertEquals(1, jdbc.queryForObject("select count(*) from workspace", Integer.class));
            throw new DataIntegrityViolationException("Simulated membership write failure");
        }).when(members).save(any(WorkspaceMember.class));

        assertThrows(DataIntegrityViolationException.class, () -> service.create("Atomic", UUID.randomUUID()));
        assertEquals(0, workspaces.count());
    }
}
