package io.boomerang.v4.controller;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.security.service.IdentityService;
import io.boomerang.v4.model.HeaderNavigationResponse;
import io.boomerang.v4.model.Navigation;
import io.boomerang.v4.service.HeaderNavigationService;
import io.boomerang.v4.service.MenuNavigationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v2/navigation")
@Tag(name = "Navigation Management",
description = "Retrieve Header and Menu Navigation.")
public class NavigationController {

  @Autowired
  MenuNavigationService menuNavigationService;

  @Autowired
  private HeaderNavigationService headerNavigationService;

  @Autowired
  private IdentityService identityService;

  @GetMapping(value = "")
  @Operation(summary = "Retrieve header navigation.")
  public HeaderNavigationResponse getHeaderNavigation() {
    return this.headerNavigationService.getHeaderNavigation(identityService.isCurrentUserAdmin());
  }

  @GetMapping(value = "/menu")
  @Operation(summary = "Retrieve menu navigation.")
  public ResponseEntity<List<Navigation>> getNavigation(@Parameter(name = "teamId", description = "The id of the Team that the user is currently on", example = "123143412312310",
      required = false) @RequestParam(required = false) Optional<String> teamId) {
    List<Navigation> response = menuNavigationService.getNavigation(identityService.isCurrentUserAdmin(), teamId);
    
    CacheControl cacheControl = CacheControl.maxAge(1, TimeUnit.HOURS);

    return ResponseEntity.ok().cacheControl(cacheControl).body(response);
  }
}
