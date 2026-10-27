package com.notespace.workspace_service.controller;

import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"test", "production", "local"})
class ProductionIdentityTest extends DefaultIdentityTest {
}
