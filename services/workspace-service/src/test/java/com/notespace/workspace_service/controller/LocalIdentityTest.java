package com.notespace.workspace_service.controller;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:local_identity;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false"})
@ActiveProfiles(value = "local", inheritProfiles = false)
class LocalIdentityTest extends WorkspaceControllerTest {
}
