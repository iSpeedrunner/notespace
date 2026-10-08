package com.notespace.workspace_service.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="workspace")
public class Workspace {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;
    private Instant createdAt;
    private Instant updatedAt;
}