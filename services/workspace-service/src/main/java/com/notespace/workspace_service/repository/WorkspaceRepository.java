package com.notespace.workspace_service.repository;

import com.notespace.workspace_service.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

import java.util.UUID;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

    @Query("select m.workspace from WorkspaceMember m where m.userId = :userId order by m.workspace.createdAt, m.workspace.id")
    List<Workspace> findAllForUser(UUID userId);

    @Query("select m.workspace from WorkspaceMember m where m.workspace.id = :workspaceId and m.userId = :userId")
    Optional<Workspace> findForUser(UUID workspaceId, UUID userId);
}
