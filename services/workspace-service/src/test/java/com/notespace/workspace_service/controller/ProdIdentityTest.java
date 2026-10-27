package com.notespace.workspace_service.controller;

import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles(value = "prod", inheritProfiles = false)
class ProdIdentityTest extends DefaultIdentityTest {
}
