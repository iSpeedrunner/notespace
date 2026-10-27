package com.notespace.workspace_service.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:default_identity;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false"})
@AutoConfigureMockMvc
@ActiveProfiles("default")
class DefaultIdentityTest {
    @Autowired MockMvc mvc;
    @Test
    void headerAndBasicAuthenticationCannotBypassMissingTrustedProvider() throws Exception {
        String api = "/api/v1/workspaces";
        mvc.perform(get(api).header("X-User-Id", UUID.randomUUID()))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("CURRENT_USER_REQUIRED"));
        mvc.perform(get(api).header("Authorization", "Basic dXNlcjpwYXNzd29yZA=="))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(api).header("X-User-Id", UUID.randomUUID()).contentType("application/json")
                        .content("{\"name\":\"Team\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get(api + "/" + UUID.randomUUID()).header("X-User-Id", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }
}
