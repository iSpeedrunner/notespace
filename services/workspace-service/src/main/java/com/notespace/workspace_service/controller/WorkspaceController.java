package com.notespace.workspace_service.controller;

import com.notespace.workspace_service.dto.CreateWorkspaceRequest;
import com.notespace.workspace_service.dto.WorkspaceResponse;
import com.notespace.workspace_service.service.CurrentUserProvider;
import com.notespace.workspace_service.service.WorkspaceApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {
    private final WorkspaceApplicationService service;
    private final CurrentUserProvider currentUser;

    @PostMapping
    public ResponseEntity<WorkspaceResponse> create(@Valid @RequestBody CreateWorkspaceRequest request) {
        var workspace = service.create(request.name(), currentUser.getCurrentUserId());
        return ResponseEntity.created(URI.create("/api/v1/workspaces/" + workspace.getId()))
                .body(WorkspaceResponse.from(workspace));
    }

    @GetMapping
    public List<WorkspaceResponse> list() {
        return service.listForUser(currentUser.getCurrentUserId()).stream().map(WorkspaceResponse::from).toList();
    }

    @GetMapping("/{workspaceId}")
    public WorkspaceResponse get(@PathVariable UUID workspaceId) {
        return WorkspaceResponse.from(service.getForUser(currentUser.getCurrentUserId(), workspaceId));
    }
}
