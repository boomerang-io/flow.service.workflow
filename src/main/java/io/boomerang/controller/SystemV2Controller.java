package io.boomerang.controller;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.model.AbstractParam;
import io.boomerang.model.Features;
import io.boomerang.model.HeaderNavigationResponse;
import io.boomerang.model.Navigation;
import io.boomerang.model.OneTimeCode;
import io.boomerang.model.Setting;
import io.boomerang.security.interceptors.AuthScope;
import io.boomerang.security.model.AuthType;
import io.boomerang.security.model.PermissionAction;
import io.boomerang.security.model.PermissionScope;
import io.boomerang.security.service.IdentityService;
import io.boomerang.service.ContextService;
import io.boomerang.service.FeatureService;
import io.boomerang.service.GlobalParamService;
import io.boomerang.service.NavigationService;
import io.boomerang.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v2")
@Tag(name = "System",
    description = "Register the instance, retrieve context and navigation, and manage global admin areas.")
public class SystemV2Controller {
  
  @Autowired
  private SettingsService settingsService;

  @Autowired
  private IdentityService identityService;
  
  @Autowired
  private GlobalParamService paramService;

  @Autowired
  NavigationService navigationService;

  @Autowired
  private ContextService contextService;

  @Autowired
  private FeatureService featureService;
  
  @GetMapping(value = "/settings")
  @AuthScope(action = PermissionAction.READ, scope = PermissionScope.SYSTEM, types = {AuthType.session, AuthType.user, AuthType.global})
  @Operation(summary = "Retrieve Boomerang Flow Settings")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public List<Setting> getAppConfiguration() {
    return settingsService.getAllSettings();
  }

  @PutMapping(value = "/settings")
  @AuthScope(action = PermissionAction.WRITE, scope = PermissionScope.SYSTEM, types = {AuthType.session, AuthType.user, AuthType.global})
  @Operation(summary = "Update Boomerang Flow Settings")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public List<Setting> updateSettings(@RequestBody List<Setting> settings) {
    return settingsService.updateSettings(settings);
  }
  
  @PutMapping(value = "/activate")
  @AuthScope(action = PermissionAction.ACTION, scope = PermissionScope.SYSTEM, types = {AuthType.session, AuthType.user})
  @Operation(summary = "Register and activate an installation of Flow")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<Boolean> register(@RequestBody(required = false) OneTimeCode otc) {
    return identityService.activateSetup(otc);
  }

  @GetMapping(value = "/context")
  @AuthScope(action = PermissionAction.READ, scope = PermissionScope.SYSTEM, types = {AuthType.session, AuthType.user, AuthType.global})
  @Operation(summary = "Retrieve this instances context, features, and navigation.")
  public HeaderNavigationResponse getHeaderNavigation() {
    return this.contextService.getHeaderNavigation(identityService.isCurrentUserAdmin());
  }
  
  @GetMapping(value = "/features")
  @AuthScope(action = PermissionAction.READ, scope = PermissionScope.SYSTEM, types = {AuthType.session, AuthType.user, AuthType.global})
  @Operation(summary = "Retrieve feature flags.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<Features> getFlowFeatures() {
    CacheControl cacheControl = CacheControl.maxAge(5, TimeUnit.MINUTES);
    return ResponseEntity.ok().cacheControl(cacheControl).body(featureService.get());
  }

  @GetMapping(value = "/navigation")
  @AuthScope(action = PermissionAction.READ, scope = PermissionScope.SYSTEM, types = {AuthType.session, AuthType.user, AuthType.global})
  @Operation(summary = "Retrieve navigation.")
  public ResponseEntity<List<Navigation>> getNavigation(@Parameter(name = "team", description = "Team as owner reference", example = "my-amazing-team",
      required = false) @RequestParam(required = false) Optional<String> team) {
    List<Navigation> response = navigationService.getNavigation(identityService.isCurrentUserAdmin(), team);
    
    CacheControl cacheControl = CacheControl.maxAge(1, TimeUnit.HOURS);

    return ResponseEntity.ok().cacheControl(cacheControl).body(response);
  }

  @GetMapping(value = "/global-params")
  @AuthScope(action = PermissionAction.READ, scope = PermissionScope.SYSTEM, types = {AuthType.session, AuthType.user, AuthType.global})
  @Operation(summary = "Get all global Params")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public List<AbstractParam> getAll() {
    return paramService.getAll();
  }

  @PostMapping(value = "/global-params")
  @AuthScope(action = PermissionAction.WRITE, scope = PermissionScope.SYSTEM, types = {AuthType.session, AuthType.user, AuthType.global})
  @Operation(summary = "Create new global Param")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public AbstractParam create(@RequestBody AbstractParam request) {
    return paramService.create(request);
  }

  @PutMapping(value = "/global-params")
  @AuthScope(action = PermissionAction.WRITE, scope = PermissionScope.SYSTEM, types = {AuthType.session, AuthType.user, AuthType.global})
  public AbstractParam update(@RequestBody AbstractParam request) {
    return paramService.update(request);
  }

  @DeleteMapping(value = "/global-params/{key}")
  @AuthScope(action = PermissionAction.DELETE, scope = PermissionScope.SYSTEM, types = {AuthType.session, AuthType.user, AuthType.global})
  @Operation(summary = "Delete specific global Param")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public void delete(@PathVariable String key) {
    paramService.delete(key);
  }
}


