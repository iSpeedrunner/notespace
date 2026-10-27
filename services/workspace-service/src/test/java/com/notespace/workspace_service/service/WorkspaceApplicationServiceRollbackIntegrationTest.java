package com.notespace.workspace_service.service;

import com.notespace.workspace_service.entity.WorkspaceMember;
import com.notespace.workspace_service.repository.WorkspaceMemberRepository;
import com.notespace.workspace_service.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ActiveProfiles("test")
class WorkspaceApplicationServiceRollbackIntegrationTest {

    @Autowired
    private WorkspaceApplicationService workspaceApplicationService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @MockitoBean
    private WorkspaceMemberRepository workspaceMemberRepository;

    @BeforeEach
    void clearDatabase() {
        workspaceRepository.deleteAll();
    }

    @Test
    void rollsBackWorkspaceWhenOwnerMembershipCannotBeSaved() {
        doThrow(new DataIntegrityViolationException("membership persistence failed"))
                .when(workspaceMemberRepository)
                .save(any(WorkspaceMember.class));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> workspaceApplicationService.create("Product Team", UUID.randomUUID())
        );

        assertEquals(0, workspaceRepository.count());
    }
}
