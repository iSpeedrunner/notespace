package com.notespace.workspace_service.controller;

import com.notespace.workspace_service.entity.WorkspaceRole;
import com.notespace.workspace_service.repository.WorkspaceMemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkspaceControllerTest {
    @Autowired MockMvc mvc;
    @Autowired WorkspaceMemberRepository members;
    static final String API = "/api/v1/workspaces";

    @Test
    void createsThenListsAndGetsWorkspaceUsingTrustedCallerOnly() throws Exception {
        String user = UUID.randomUUID().toString();
        String spoof = UUID.randomUUID().toString();
        var result = mvc.perform(post(API).header("X-User-Id", user).contentType("application/json")
                        .content("{\"name\":\"  My Workspace  \",\"ownerUserId\":\"" + spoof + "\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("My Workspace"))
                .andExpect(jsonPath("$.id").isString()).andExpect(jsonPath("$.createdAt").isString())
                .andExpect(jsonPath("$.updatedAt").isString()).andReturn();
        String location = result.getResponse().getHeader("Location");
        assertNotNull(location);
        UUID id = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
        var membership = members.findAll().stream().filter(m -> m.getWorkspace().getId().equals(id)).findFirst().orElseThrow();
        assertEquals(UUID.fromString(user), membership.getUserId());
        assertEquals(WorkspaceRole.OWNER, membership.getRole());
        mvc.perform(get(API).header("X-User-Id", user)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()));
        mvc.perform(get(location).header("X-User-Id", user)).andExpect(status().isOk());
        mvc.perform(get(location).header("X-User-Id", spoof)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("WORKSPACE_NOT_FOUND"));
        mvc.perform(get(API + "/" + UUID.randomUUID()).header("X-User-Id", user))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("WORKSPACE_NOT_FOUND"));
        mvc.perform(get(API).header("X-User-Id", spoof)).andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"name\":null}", "{\"name\":\"\"}", "{\"name\":\"   \"}", "{", "{\"name\":\"\\u2003\"}"})
    void invalidRequestsReturnSafeJson(String body) throws Exception {
        mvc.perform(post(API).header("X-User-Id", UUID.randomUUID()).contentType("application/json").content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value(API)).andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void enforcesNameLengthAndUuidFormat() throws Exception {
        mvc.perform(post(API).header("X-User-Id", UUID.randomUUID()).contentType("application/json")
                        .content("{\"name\":\"" + "a".repeat(101) + "\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(API + "/invalid").header("X-User-Id", UUID.randomUUID()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("INVALID_UUID"));
    }

    @Test
    void frameworkClientErrorsKeepTheirHttpStatus() throws Exception {
        String user = UUID.randomUUID().toString();
        mvc.perform(put(API).header("X-User-Id", user)).andExpect(status().isMethodNotAllowed());
        mvc.perform(post(API).header("X-User-Id", user).contentType("text/plain").content("Team"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(get(API + "/unmatched/path").header("X-User-Id", user)).andExpect(status().isNotFound());
    }

    @Test
    void absentMalformedDuplicateOrUnverifiedIdentityIsUnauthorized() throws Exception {
        mvc.perform(get(API)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        mvc.perform(post(API).contentType("application/json").content("{\"name\":\"Team\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get(API).header("Authorization", "Bearer unverified")).andExpect(status().isUnauthorized());
        for (String header : new String[]{"", "invalid", "1-1-1-1-1", " " + UUID.randomUUID()}) {
            mvc.perform(get(API).header("X-User-Id", header)).andExpect(status().isUnauthorized());
        }
        mvc.perform(get(API).header("X-User-Id", UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }
}
