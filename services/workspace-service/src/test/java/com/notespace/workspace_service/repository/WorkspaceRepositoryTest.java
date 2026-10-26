package com.notespace.workspace_service.repository;

import com.notespace.workspace_service.entity.Workspace;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class WorkspaceRepositoryTest {

    @Autowired
    private WorkspaceRepository workspaceRepository;


    @Test
    void shouldSaveWorkspace() {
        Workspace workspace = new Workspace();

        workspace.setName("Test Workspace");
        workspace.setCreatedAt(Instant.now());
        workspace.setUpdatedAt(Instant.now());

        Workspace saved = workspaceRepository.saveAndFlush(workspace);

        assertNotNull(saved.getId());
    }
}
