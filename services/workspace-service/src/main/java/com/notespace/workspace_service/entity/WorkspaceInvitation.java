package com.notespace.workspace_service.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workspace_invitation")
public class WorkspaceInvitation {
    @Id
    private UUID id;

    @ManyToOne
    private Workspace workspace;

    private UUID inviteeUserId;

    private UUID invitedByUserId;

    @Enumerated(EnumType.STRING)
    private WorkspaceInvitationStatus status;

    private Instant expiresAt;

    private Instant createdAt;

    private Instant respondedAt;
}
