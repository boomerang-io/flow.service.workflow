package net.boomerangplatform.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(value = "flow.mode", havingValue = "standalone", matchIfMissing = false)
public class UserManagementController {



}
