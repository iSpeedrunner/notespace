package com.notespace.workspace_service.entity;


import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "workspace_member",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"workspace_id", "user_id"}
                )
        }
)
public class WorkspaceMember {
    @Id
    private UUID id;

    @ManyToOne
    private Workspace workspace;

    private UUID userId;

    @Enumerated(EnumType.STRING)
    private WorkspaceRole role;

    private Instant joinedAt;
}